package com.examflow.grading.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.util.AesUtil;
import com.examflow.grading.client.PaperServiceClient;
import com.examflow.grading.dto.ExamSnapshot;
import com.examflow.grading.dto.GradingTaskVO;
import com.examflow.grading.entity.AnswerDetailView;
import com.examflow.grading.entity.ExamSessionView;
import com.examflow.grading.entity.GradingRecord;
import com.examflow.grading.entity.GradingTask;
import com.examflow.grading.entity.RegistrationView;
import com.examflow.grading.mapper.AnswerDetailViewMapper;
import com.examflow.grading.mapper.ExamSessionViewMapper;
import com.examflow.grading.mapper.GradingRecordMapper;
import com.examflow.grading.mapper.GradingTaskMapper;
import com.examflow.grading.mapper.RegistrationViewMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 主观题评阅(FR-GRADE-02/03/05):任务分派(脱敏)、双评/仲裁、进度。
 * 双评:分差 ≤ 阈值(满分 20%)取均值;超限进入第三评仲裁(仲裁分生效)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Set<String> SUBJECTIVE_TYPES = Set.of("subjective", "case", "operation");
    /** 双评分差阈值比例(满分 20%),后续参数化到 sys_param。 */
    private static final BigDecimal DIFF_RATIO = BigDecimal.valueOf(0.2);

    private final GradingTaskMapper taskMapper;
    private final GradingRecordMapper recordMapper;
    private final ExamSessionViewMapper sessionMapper;
    private final AnswerDetailViewMapper answerMapper;
    private final RegistrationViewMapper registrationMapper;
    private final PaperServiceClient paperClient;
    private final ObjectMapper objectMapper;
    private final ScoreService scoreService;

    /** 分派:按考次将主观题生成为双评任务(已存在的跳过)。 */
    @Transactional
    public int assignTasks(Long planId, List<Long> graderIds) {
        // 双评 + 仲裁:一评/二评/仲裁须由不同人担任(uk_task_grader 防重复评分)
        if (graderIds == null || graderIds.size() < 3) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "至少指定 3 名阅卷员(一评/二评/仲裁)");
        }
        List<RegistrationView> regs = registrationMapper.selectList(Wrappers.lambdaQuery(RegistrationView.class)
                .eq(RegistrationView::getPlanId, planId)
                .eq(RegistrationView::getStatus, "approved"));
        int created = 0;
        for (RegistrationView reg : regs) {
            ExamSessionView session = sessionMapper.selectOne(Wrappers.lambdaQuery(ExamSessionView.class)
                    .eq(ExamSessionView::getRegistrationId, reg.getId()));
            if (session == null) {
                continue;
            }
            ExamSnapshot snap = paperClient.examSnapshot(session.getPaperSnapshotId(), true);
            for (ExamSnapshot.ExamQuestion q : snap.questions()) {
                if (!SUBJECTIVE_TYPES.contains(q.type())) {
                    continue;
                }
                Long exists = taskMapper.selectCount(Wrappers.lambdaQuery(GradingTask.class)
                        .eq(GradingTask::getSessionId, session.getId())
                        .eq(GradingTask::getQuestionSeq, q.seq()));
                if (exists > 0) {
                    continue;
                }
                GradingTask task = new GradingTask();
                task.setSessionId(session.getId());
                task.setQuestionSeq(q.seq());
                task.setGraderIds(toJson(graderIds.subList(0, 3)));
                task.setRound("first");
                task.setStatus("pending");
                task.setCreateTime(LocalDateTime.now());
                taskMapper.insert(task);
                created++;
            }
        }
        log.info("评阅任务分派: plan={}, 生成任务 {} 个", planId, created);
        return created;
    }

    /** 我的可评任务(脱敏:不含考生身份信息)。 */
    public List<GradingTaskVO> myTasks(Long graderId) {
        List<GradingTask> tasks = taskMapper.selectList(Wrappers.lambdaQuery(GradingTask.class)
                .ne(GradingTask::getStatus, "graded"));
        List<GradingTaskVO> result = new ArrayList<>();
        for (GradingTask task : tasks) {
            if (!currentGrader(task).equals(graderId)) {
                continue;
            }
            result.add(toVO(task));
        }
        return result;
    }

    /** 评分:双评/仲裁流转(FR-GRADE-03)。 */
    @Transactional
    public void submitScore(Long taskId, Long graderId, BigDecimal score, String comment) {
        GradingTask task = requireTask(taskId);
        if ("graded".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "任务已完成评分");
        }
        if (!currentGrader(task).equals(graderId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "当前轮次阅卷员不符");
        }
        if (score == null || score.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "评分不合法");
        }
        GradingRecord record = new GradingRecord();
        record.setTaskId(taskId);
        record.setGraderId(graderId);
        record.setScore(score);
        record.setComment(comment);
        record.setSubmitTime(LocalDateTime.now());
        recordMapper.insert(record);

        switch (task.getRound()) {
            case "first" -> {
                task.setRound("second");
                task.setStatus("grading");
                taskMapper.updateById(task);
            }
            case "second" -> {
                List<BigDecimal> scores = scoresOf(taskId);
                BigDecimal diff = scores.get(0).subtract(scores.get(1)).abs();
                BigDecimal fullScore = fullScoreOf(task);
                if (diff.compareTo(fullScore.multiply(DIFF_RATIO)) <= 0) {
                    // 分差在阈值内:取均值,任务完成
                    BigDecimal finalScore = scores.get(0).add(scores.get(1))
                            .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                    complete(task, finalScore);
                } else {
                    // 超限:进入第三评仲裁
                    task.setRound("arbitration");
                    task.setStatus("grading");
                    taskMapper.updateById(task);
                    log.info("双评分差超限,进入仲裁: task={}, 分差={}", taskId, diff);
                }
            }
            case "arbitration" -> complete(task, score);
            default -> throw new BusinessException(ErrorCode.BIZ_ERROR, "未知轮次");
        }
    }

    /** 评阅进度(按考次)。 */
    public Map<String, Object> progress(Long planId) {
        List<RegistrationView> regs = registrationMapper.selectList(Wrappers.lambdaQuery(RegistrationView.class)
                .eq(RegistrationView::getPlanId, planId)
                .eq(RegistrationView::getStatus, "approved"));
        long sessions = regs.size();
        long total = 0;
        long pending = 0;
        long grading = 0;
        long graded = 0;
        for (RegistrationView reg : regs) {
            ExamSessionView session = sessionMapper.selectOne(Wrappers.lambdaQuery(ExamSessionView.class)
                    .eq(ExamSessionView::getRegistrationId, reg.getId()));
            if (session == null) {
                continue;
            }
            List<GradingTask> tasks = taskMapper.selectList(Wrappers.lambdaQuery(GradingTask.class)
                    .eq(GradingTask::getSessionId, session.getId()));
            total += tasks.size();
            pending += tasks.stream().filter(t -> "pending".equals(t.getStatus())).count();
            grading += tasks.stream().filter(t -> "grading".equals(t.getStatus())).count();
            graded += tasks.stream().filter(t -> "graded".equals(t.getStatus())).count();
        }
        return Map.of("sessions", sessions, "total", total, "pending", pending,
                "grading", grading, "graded", graded);
    }

    // ---------- 辅助 ----------

    /** 任务完成:写分 + 结算会话成绩。 */
    private void complete(GradingTask task, BigDecimal finalScore) {
        task.setStatus("graded");
        taskMapper.updateById(task);
        // 写 answer_detail 得分
        AnswerDetailView detail = answerMapper.selectOne(Wrappers.lambdaQuery(AnswerDetailView.class)
                .eq(AnswerDetailView::getSessionId, task.getSessionId())
                .eq(AnswerDetailView::getQuestionSeq, task.getQuestionSeq()));
        if (detail != null) {
            detail.setScore(finalScore);
            detail.setScoreStatus("graded");
            answerMapper.updateById(detail);
        }
        log.info("评阅完成: task={}, 最终分={}", task.getId(), finalScore);
        // 会话主观题全部评完 → 结算总分与及格判定
        scoreService.settleIfComplete(task.getSessionId());
    }

    /** 当前轮次阅卷员:first→[0], second→[1], arbitration→[2](三人互不相同)。 */
    private Long currentGrader(GradingTask task) {
        List<Long> graders = parseIds(task.getGraderIds());
        return switch (task.getRound()) {
            case "second" -> graders.get(1);
            case "arbitration" -> graders.get(2);
            default -> graders.get(0);
        };
    }

    private List<BigDecimal> scoresOf(Long taskId) {
        return recordMapper.selectList(Wrappers.lambdaQuery(GradingRecord.class)
                        .eq(GradingRecord::getTaskId, taskId))
                .stream().map(GradingRecord::getScore).toList();
    }

    private BigDecimal fullScoreOf(GradingTask task) {
        ExamSnapshot snap = paperClient.examSnapshot(sessionMapper.selectById(task.getSessionId())
                .getPaperSnapshotId(), true);
        return snap.questions().stream()
                .filter(q -> q.seq().equals(task.getQuestionSeq()))
                .findFirst().map(ExamSnapshot.ExamQuestion::score)
                .orElse(BigDecimal.ZERO);
    }

    private GradingTaskVO toVO(GradingTask task) {
        ExamSessionView session = sessionMapper.selectById(task.getSessionId());
        ExamSnapshot snap = paperClient.examSnapshot(session.getPaperSnapshotId(), true);
        ExamSnapshot.ExamQuestion q = snap.questions().stream()
                .filter(x -> x.seq().equals(task.getQuestionSeq())).findFirst().orElse(null);
        AnswerDetailView detail = answerMapper.selectOne(Wrappers.lambdaQuery(AnswerDetailView.class)
                .eq(AnswerDetailView::getSessionId, task.getSessionId())
                .eq(AnswerDetailView::getQuestionSeq, task.getQuestionSeq()));
        return new GradingTaskVO(task.getId(), task.getQuestionSeq(),
                q == null ? null : q.type(),
                q == null ? null : q.stem(),
                q == null ? null : q.options(),
                q == null ? null : q.score(),
                q == null ? null : q.answer(),   // 采分点(标准答案)
                detail == null ? null : decrypt(detail.getAnswer()),
                task.getRound(), task.getStatus());
    }

    private GradingTask requireTask(Long id) {
        GradingTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评阅任务不存在");
        }
        return task;
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化失败");
        }
    }

    private List<Long> parseIds(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String decrypt(String cipher) {
        try {
            return AesUtil.decrypt(cipher);
        } catch (Exception e) {
            return null;
        }
    }
}
