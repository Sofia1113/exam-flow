package com.examflow.exam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.util.AesUtil;
import com.examflow.exam.client.GradingServiceClient;
import com.examflow.exam.client.PaperServiceClient;
import com.examflow.exam.domain.ExamSessionState;
import com.examflow.exam.dto.ExamDTO;
import com.examflow.exam.dto.ExamSnapshot;
import com.examflow.exam.entity.AnswerDetail;
import com.examflow.exam.entity.ExamPlanView;
import com.examflow.exam.entity.ExamRegistration;
import com.examflow.exam.entity.ExamSession;
import com.examflow.exam.entity.ExamSlotView;
import com.examflow.exam.mapper.AnswerDetailMapper;
import com.examflow.exam.mapper.ExamPlanMapper;
import com.examflow.exam.mapper.ExamSessionMapper;
import com.examflow.exam.mapper.ExamSlotMapper;
import com.examflow.exam.mapper.RegistrationMapper;
import com.examflow.exam.service.ExamSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 考试会话核心实现(FR-EXAM / TDD §3、§6)。
 *
 * <ul>
 *   <li>进入:报名/场次时间窗校验 → 快照抽卷(seed 可复现)→ 创建会话;</li>
 *   <li>保存:seq 对齐(增量幂等 upsert),答案加密存储;</li>
 *   <li>心跳:Redis 在线状态(TTL 60s);</li>
 *   <li>交卷:setnx 幂等锁 + 状态机 + 落库后触发判分(grading 服务);</li>
 *   <li>恢复:进入次数上限 + 服务器时间计算剩余时长。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private static final int MAX_ENTER_COUNT = 3;
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(60);

    private final ExamSessionMapper sessionMapper;
    private final AnswerDetailMapper answerMapper;
    private final RegistrationMapper registrationMapper;
    private final ExamSlotMapper slotMapper;
    private final ExamPlanMapper planMapper;
    private final PaperServiceClient paperClient;
    private final GradingServiceClient gradingClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ExamDTO.EnterResp enter(Long registrationId, String clientIp, String deviceFp) {
        // 1. 报名资格
        ExamRegistration reg = registrationMapper.selectById(registrationId);
        if (reg == null || !"approved".equals(reg.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "报名未通过审核,无法进入考试");
        }
        // 2. 场次时间窗:开考前 30 分钟可进,开考后 30 分钟禁止入场(FR-EXAM-01)
        ExamSlotView slot = slotMapper.selectById(reg.getSlotId());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(slot.getStartTime().minusMinutes(30))) {
            throw new BusinessException(ErrorCode.EXAM_NOT_STARTED, "考试尚未开始,开考前 30 分钟可进入");
        }
        if (now.isAfter(slot.getStartTime().plusMinutes(30))) {
            throw new BusinessException(ErrorCode.EXAM_LATE);
        }
        // 3. 会话幂等:已存在会话不允许重复进入
        ExamSession exist = sessionMapper.selectOne(Wrappers.lambdaQuery(ExamSession.class)
                .eq(ExamSession::getRegistrationId, registrationId));
        if (exist != null) {
            if (!"ANSWERING".equals(exist.getStatus())) {
                throw new BusinessException(ErrorCode.EXAM_SESSION_SUBMITTED);
            }
            throw new BusinessException(ErrorCode.BIZ_ERROR, "您已进入考试,请使用恢复会话接口");
        }
        // 4. 拉取快照并抽卷(seed 可复现,TDD §6.3)
        ExamPlanView plan = planMapper.selectById(reg.getPlanId());
        ExamSnapshot snap = paperClient.examSnapshot(plan.getPaperId(), false);
        String seed = sha256(snap.paperId() + ":" + reg.getSlotId() + ":" + registrationId);
        List<ExamSnapshot.ExamQuestion> drawn = draw(snap, seed);
        // 5. 创建会话
        ExamSession session = new ExamSession();
        session.setSessionNo("SES-" + ThreadLocalRandom.current().nextLong(1_000_000, 9_999_999));
        session.setRegistrationId(registrationId);
        session.setSlotId(reg.getSlotId());
        session.setPaperSnapshotId(snap.paperId());
        session.setSeed(seed);
        session.setQuestionIds(encrypt(toDrawJson(drawn)));
        session.setStatus(ExamSessionState.ANSWERING.name());
        session.setStartedAt(now);
        session.setDeadlineAt(now.plusMinutes(snap.durationMin()));
        session.setEnterCount(1);
        session.setLastSeq(0L);
        session.setClientIp(clientIp);
        session.setDeviceFp(deviceFp);
        sessionMapper.insert(session);
        log.info("进入考试: session={}, registration={}, 抽题 {} 道", session.getId(), registrationId, drawn.size());
        return new ExamDTO.EnterResp(session.getId(), session.getDeadlineAt(),
                snap.durationMin(), toClientQuestions(drawn));
    }

    @Override
    public ExamDTO.ResumeResp resume(Long sessionId, Long registrationId) {
        ExamSession session = requireOwnedSession(sessionId, registrationId);
        if (!session.getStatus().equals(ExamSessionState.ANSWERING.name())) {
            throw new BusinessException(ErrorCode.EXAM_SESSION_SUBMITTED);
        }
        if (session.getEnterCount() >= MAX_ENTER_COUNT) {
            throw new BusinessException(ErrorCode.EXAM_ENTER_LIMIT);
        }
        session.setEnterCount(session.getEnterCount() + 1);
        sessionMapper.updateById(session);
        log.info("恢复会话: session={}, 第 {} 次进入", sessionId, session.getEnterCount());

        List<ExamSnapshot.ExamQuestion> questions = drawQuestionsFromSession(session);
        List<ExamDTO.SavedAnswer> saved = answerMapper.selectList(Wrappers.lambdaQuery(AnswerDetail.class)
                        .eq(AnswerDetail::getSessionId, sessionId))
                .stream()
                .map(a -> new ExamDTO.SavedAnswer(a.getQuestionSeq(), decrypt(a.getAnswer())))
                .toList();
        long remainSeconds = Math.max(0, Duration.between(LocalDateTime.now(), session.getDeadlineAt()).toSeconds());
        return new ExamDTO.ResumeResp(sessionId, session.getStatus(), session.getDeadlineAt(),
                remainSeconds, session.getLastSeq(), toClientQuestions(questions), saved);
    }

    @Override
    public long saveAnswers(Long sessionId, Long registrationId, long fromSeq,
                            List<Map<String, Object>> answers) {
        ExamSession session = requireOwnedSession(sessionId, registrationId);
        if (!session.getStatus().equals(ExamSessionState.ANSWERING.name())) {
            throw new BusinessException(ErrorCode.EXAM_SESSION_SUBMITTED);
        }
        if (LocalDateTime.now().isAfter(session.getDeadlineAt())) {
            throw new BusinessException(ErrorCode.EXAM_ALREADY_CLOSED, "考试时间已到,请交卷");
        }
        long lastSeq = applyAnswers(session, answers);
        session.setLastSeq(lastSeq);
        sessionMapper.updateById(session);
        return lastSeq;
    }

    /** 批量增量 upsert(seq 幂等);交卷与保存共用,交卷不检查超时。 */
    private long applyAnswers(ExamSession session, List<Map<String, Object>> answers) {
        long lastSeq = session.getLastSeq();
        for (Map<String, Object> item : answers) {
            long seq = ((Number) item.get("seq")).longValue();
            int questionSeq = ((Number) item.get("questionSeq")).intValue();
            String answer = (String) item.get("answer");
            if (seq <= lastSeq) {
                continue; // 重复增量,幂等忽略
            }
            AnswerDetail detail = answerMapper.selectOne(Wrappers.lambdaQuery(AnswerDetail.class)
                    .eq(AnswerDetail::getSessionId, session.getId())
                    .eq(AnswerDetail::getQuestionSeq, questionSeq));
            if (detail == null) {
                detail = new AnswerDetail();
                detail.setSessionId(session.getId());
                detail.setQuestionSeq(questionSeq);
                detail.setAnswer(encrypt(answer));
                detail.setScoreStatus("pending");
                detail.setVersion(1);
                answerMapper.insert(detail);
            } else {
                detail.setAnswer(encrypt(answer));
                detail.setVersion(detail.getVersion() + 1);
                answerMapper.updateById(detail);
            }
            lastSeq = Math.max(lastSeq, seq);
        }
        return lastSeq;
    }

    @Override
    public void heartbeat(Long sessionId, Long registrationId) {
        requireOwnedSession(sessionId, registrationId);
        redis.opsForValue().set("exam:online:" + sessionId, "1", HEARTBEAT_TTL);
    }

    @Override
    @Transactional
    public void submit(Long sessionId, Long registrationId, List<Map<String, Object>> answers, String sign) {
        // 幂等:Redis setnx 锁(60s)+ 状态机双保险
        Boolean locked = redis.opsForValue().setIfAbsent("exam:submit-lock:" + sessionId, "1",
                Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.EXAM_SUBMIT_CONFLICT);
        }
        try {
            ExamSession session = requireOwnedSession(sessionId, registrationId);
            if (!session.getStatus().equals(ExamSessionState.ANSWERING.name())) {
                throw new BusinessException(ErrorCode.EXAM_SESSION_SUBMITTED);
            }
            // 全量增量落库(与保存链路同一 upsert 逻辑;交卷不检查超时,自动交卷场景)
            long lastSeq = applyAnswers(session, answers);
            session.setLastSeq(lastSeq);
            // 状态迁移并落定交卷时间(先落库后 ack,客户端可重试)
            session.setStatus(ExamSessionState.SUBMITTED.name());
            session.setSubmitTime(LocalDateTime.now());
            sessionMapper.updateById(session);
            log.info("交卷成功: session={}, registration={}", sessionId, registrationId);
            // 判分必须在交卷事务提交后触发(afterCommit):若在事务内同步调用,
            // grading 更新同一 exam_session 行会与交卷事务死锁(锁等待超时 50s)。
            // 生产:TDD §3.3 交卷完成事件走 RocketMQ 异步削峰,失败由补偿任务兜底。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        gradingClient.grade(sessionId);
                    } catch (Exception e) {
                        log.error("判分触发失败(待补偿): session={}", sessionId, e);
                    }
                }
            });
        } finally {
            redis.delete("exam:submit-lock:" + sessionId);
        }
    }

    // ---------- 抽卷(可复现,TDD §6.3) ----------

    /** 固定卷:全量按序;策略卷:按槽位(乱序组)hash 排序取 count。 */
    private List<ExamSnapshot.ExamQuestion> draw(ExamSnapshot snap, String seed) {
        if (snap.blueprint() == null || snap.blueprint().isBlank()) {
            return snap.questions().stream()
                    .sorted(Comparator.comparing(ExamSnapshot.ExamQuestion::seq)).toList();
        }
        try {
            var blueprint = objectMapper.readTree(snap.blueprint());
            List<ExamSnapshot.ExamQuestion> result = new ArrayList<>();
            int group = 1;
            for (var slot : blueprint.path("slots")) {
                int count = slot.path("count").asInt();
                int g = group++;
                List<ExamSnapshot.ExamQuestion> pool = snap.questions().stream()
                        .filter(q -> q.shuffleGroup() != null && q.shuffleGroup() == g)
                        .sorted(Comparator.comparingInt(q -> hash(seed, g, q.pqId())))
                        .toList();
                if (pool.size() < count) {
                    throw new BusinessException(ErrorCode.BIZ_ERROR,
                            "题池不足:槽位 " + g + " 需 " + count + " 题,仅 " + pool.size() + " 题可用");
                }
                result.addAll(pool.subList(0, count));
            }
            // 卷面顺序:按题型槽位序
            result.sort(Comparator.comparing(ExamSnapshot.ExamQuestion::seq));
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "抽卷失败");
        }
    }

    /** 确定性 hash(seed, salt) → 排序键,同种子同结果(审计可重放)。 */
    private int hash(String seed, int salt, Long pqId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((seed + ":" + salt + ":" + pqId).getBytes(StandardCharsets.UTF_8));
            return ((digest[0] & 0xFF) << 24) | ((digest[1] & 0xFF) << 16)
                    | ((digest[2] & 0xFF) << 8) | (digest[3] & 0xFF);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- 辅助 ----------

    private ExamSession requireOwnedSession(Long sessionId, Long registrationId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getRegistrationId().equals(registrationId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    /** 从会话抽题结果还原题目内容(快照重新拉取,保证恢复场景完整)。 */
    private List<ExamSnapshot.ExamQuestion> drawQuestionsFromSession(ExamSession session) {
        ExamSnapshot snap = paperClient.examSnapshot(session.getPaperSnapshotId(), false);
        List<Long> pqIds = fromDrawJson(decrypt(session.getQuestionIds())).stream()
                .map(DrawItem::pqId).toList();
        Map<Long, ExamSnapshot.ExamQuestion> byId = snap.questions().stream()
                .collect(Collectors.toMap(ExamSnapshot.ExamQuestion::pqId, q -> q));
        return pqIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    private List<ExamDTO.ClientQuestion> toClientQuestions(List<ExamSnapshot.ExamQuestion> questions) {
        return questions.stream()
                .map(q -> new ExamDTO.ClientQuestion(q.seq(), q.type(), q.stem(), q.options(), q.score()))
                .toList();
    }

    private String toDrawJson(List<ExamSnapshot.ExamQuestion> drawn) {
        try {
            List<Map<String, Object>> items = drawn.stream().map(q -> {
                Map<String, Object> m = new HashMap<>();
                m.put("pqId", q.pqId());
                m.put("seq", q.seq());
                m.put("score", q.score());
                return m;
            }).toList();
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "抽题结果序列化失败");
        }
    }

    private List<DrawItem> fromDrawJson(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, DrawItem.class));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "抽题结果解析失败");
        }
    }

    private String encrypt(String plain) {
        try {
            return AesUtil.encrypt(plain);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据加密失败");
        }
    }

    private String decrypt(String cipher) {
        try {
            return AesUtil.decrypt(cipher);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据解密失败");
        }
    }

    public record DrawItem(Long pqId, Integer seq, Number score) {
    }
}
