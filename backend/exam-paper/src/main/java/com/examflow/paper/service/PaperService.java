package com.examflow.paper.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.util.AesUtil;
import com.examflow.paper.client.QuestionServiceClient;
import com.examflow.paper.dto.PaperDetailVO;
import com.examflow.paper.dto.PaperVO;
import com.examflow.paper.dto.QuestionSnapshot;
import com.examflow.paper.entity.Paper;
import com.examflow.paper.entity.PaperQuestion;
import com.examflow.paper.entity.PaperSnapshot;
import com.examflow.paper.mapper.PaperMapper;
import com.examflow.paper.mapper.PaperQuestionMapper;
import com.examflow.paper.mapper.PaperSnapshotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 组卷服务(FR-PAPER-01/02/05/06)。
 * 固定组卷:逐题挑选;策略组卷:蓝图(槽位模型)自动抽题。
 * 发布时生成不可变快照(题目内容快照,后续题目修改不影响已发布考试)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final PaperSnapshotMapper snapshotMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionServiceClient questionClient;
    private final ObjectMapper objectMapper;

    public PageResult<PaperVO> page(long page, long size, Long subjectId, String paperType, String status) {
        var wrapper = Wrappers.lambdaQuery(Paper.class);
        if (subjectId != null) {
            wrapper.eq(Paper::getSubjectId, subjectId);
        }
        if (paperType != null && !paperType.isBlank()) {
            wrapper.eq(Paper::getPaperType, paperType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Paper::getStatus, status);
        }
        wrapper.orderByDesc(Paper::getId);
        Page<Paper> p = paperMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.convert(this::toVO));
    }

    /** 创建试卷(固定/策略)。 */
    @Transactional
    public Long create(PaperReq req) {
        if (req.name() == null || req.name().isBlank() || req.subjectId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "试卷名称与科目必填");
        }
        Paper paper = new Paper();
        paper.setName(req.name());
        paper.setSubjectId(req.subjectId());
        paper.setPassScore(req.passScore() == null ? BigDecimal.valueOf(60) : req.passScore());
        paper.setDurationMin(req.durationMin() == null ? 120 : req.durationMin());
        paper.setStatus("draft");

        if ("fixed".equals(req.paperType())) {
            paper.setPaperType("fixed");
            // 选题列表持久化到 blueprint(JSON):{"questions":[{"questionId":1,"score":2}]}
            paper.setBlueprint(serializeFixedQuestions(req.questions()));
            paper.setTotalScore(req.questions().stream()
                    .map(PaperReq.QuestionItem::score)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        } else {
            paper.setPaperType("strategy");
            paper.setBlueprint(validateBlueprint(req.blueprint()));
            paper.setTotalScore(blueprintTotal(req.blueprint()));
        }
        paperMapper.insert(paper);
        log.info("创建试卷: id={}, name={}, type={}, total={}", paper.getId(), paper.getName(), paper.getPaperType(), paper.getTotalScore());
        return paper.getId();
    }

    /** 更新(仅草稿/待审;已审或已发布禁止)。 */
    @Transactional
    public void update(Long id, PaperReq req) {
        Paper paper = require(id);
        if (!List.of("draft", "pending").contains(paper.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "当前状态不可修改,请归档后重新创建");
        }
        if (req.name() != null) {
            paper.setName(req.name());
        }
        if (req.passScore() != null) {
            paper.setPassScore(req.passScore());
        }
        if (req.durationMin() != null) {
            paper.setDurationMin(req.durationMin());
        }
        paperMapper.updateById(paper);
    }

    /** 送审:draft → pending。 */
    @Transactional
    public void submit(Long id) {
        Paper paper = require(id);
        if (!"draft".equals(paper.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅草稿状态可送审");
        }
        paper.setStatus("pending");
        paperMapper.updateById(paper);
    }

    /** 审批:待审 → 已审(通过)/草稿(驳回)。 */
    @Transactional
    public void audit(Long id, boolean pass, String opinion, Long operatorId) {
        Paper paper = require(id);
        if (!"pending".equals(paper.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅待审状态可审核");
        }
        paper.setStatus(pass ? "approved" : "draft");
        paper.setApproverId(operatorId);
        paper.setApproveTime(LocalDateTime.now());
        paper.setApproveOpinion(opinion);
        paperMapper.updateById(paper);
    }

    /** 发布:已审 → 已发布,生成不可变快照(FR-PAPER-06,TDD §4.2.2)。 */
    @Transactional
    public void publish(Long id) {
        Paper paper = require(id);
        if (!"approved".equals(paper.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅已审状态可发布");
        }
        PaperSnapshot snapshot = new PaperSnapshot();
        snapshot.setPaperId(paper.getId());
        snapshot.setSnapshotNo("SNAP-" + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + paper.getId());
        snapshot.setContent(encrypt(buildSnapshotContent(paper)));
        snapshot.setTotalScore(paper.getTotalScore());
        snapshot.setVersion(1);
        snapshot.setStatus("active");
        snapshot.setCreateTime(LocalDateTime.now());
        snapshotMapper.insert(snapshot);
        // 固定卷:按选定题目生成快照明细;策略卷:候选题池全量快照
        if ("fixed".equals(paper.getPaperType())) {
            snapshotFixedQuestions(snapshot.getId(), paper);
        } else {
            snapshotStrategyPool(snapshot.getId(), paper);
        }
        paper.setStatus("published");
        paperMapper.updateById(paper);
        log.info("试卷发布并生成快照: paper={}, snapshot={}", paper.getId(), snapshot.getId());
    }

    /** 归档(历史卷不可再考试)。 */
    @Transactional
    public void archive(Long id) {
        Paper paper = require(id);
        if (!"published".equals(paper.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅已发布状态可归档");
        }
        paper.setStatus("archived");
        paperMapper.updateById(paper);
    }

    /** 预览/详情:已发布读快照,未发布实时组装。withAnswer=true 返回解密答案。 */
    public PaperDetailVO detail(Long id, boolean withAnswer) {
        Paper paper = require(id);
        PaperVO vo = toVO(paper);
        List<PaperDetailVO.PreviewQuestion> questions = new ArrayList<>();
        if ("published".equals(paper.getStatus())) {
            PaperSnapshot snapshot = snapshotMapper.selectOne(Wrappers.lambdaQuery(PaperSnapshot.class)
                    .eq(PaperSnapshot::getPaperId, paper.getId()).orderByDesc(PaperSnapshot::getId).last("LIMIT 1"));
            if (snapshot != null) {
                questions = snapshotQuestions(snapshot.getId(), withAnswer);
            }
        } else if ("fixed".equals(paper.getPaperType())) {
            questions = fixedPreview(paper, withAnswer);
        }
        return new PaperDetailVO(vo, paper.getBlueprint(), questions);
    }

    /** 固定卷未发布预览:从题库实时取题。 */
    private List<PaperDetailVO.PreviewQuestion> fixedPreview(Paper paper, boolean withAnswer) {
        try {
            JsonNode content = objectMapper.readTree(decrypt(buildSnapshotContent(paper)));
            List<Long> ids = new ArrayList<>();
            for (JsonNode q : content.path("questions")) {
                ids.add(q.path("questionId").asLong());
            }
            Map<Long, QuestionSnapshot> snapshots = questionClient.batch(ids).stream()
                    .collect(java.util.stream.Collectors.toMap(QuestionSnapshot::questionId, s -> s));
            List<PaperDetailVO.PreviewQuestion> result = new ArrayList<>();
            int seq = 1;
            for (JsonNode q : content.path("questions")) {
                QuestionSnapshot s = snapshots.get(q.path("questionId").asLong());
                if (s != null) {
                    result.add(new PaperDetailVO.PreviewQuestion(seq++, s.type(), s.stem(), s.options(),
                            withAnswer ? s.answer() : null, withAnswer ? s.analysis() : null,
                            q.path("score").decimalValue(), 0));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "预览组装失败");
        }
    }

    private void snapshotFixedQuestions(Long snapshotId, Paper paper) {
        try {
            JsonNode content = objectMapper.readTree(buildSnapshotContent(paper));
            int seq = 1;
            for (JsonNode q : content.path("questions")) {
                QuestionSnapshot s = questionClient.batch(List.of(q.path("questionId").asLong())).stream()
                        .findFirst().orElseThrow(() -> new BusinessException(ErrorCode.BIZ_ERROR,
                        "题目不存在: " + q.path("questionId").asLong()));
                insertSnapshotQuestion(snapshotId, seq++, q.path("score").decimalValue(), 0, s);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "快照生成失败");
        }
    }

    /** 策略卷:候选题池全量快照(shuffle_group 按槽位,考试时按种子抽题)。 */
    private void snapshotStrategyPool(Long snapshotId, Paper paper) {
        try {
            JsonNode blueprint = objectMapper.readTree(paper.getBlueprint());
            int shuffleGroup = 1;
            for (JsonNode slot : blueprint.path("slots")) {
                List<QuestionSnapshot> pool = questionClient.pool(paper.getSubjectId(),
                        slot.path("type").asText(),
                        slot.hasNonNull("difficulty") ? slot.path("difficulty").asInt() : null,
                        slot.hasNonNull("knowledge") ? slot.path("knowledge").asText() : null);
                for (QuestionSnapshot s : pool) {
                    insertSnapshotQuestion(snapshotId, 0, slot.path("score").decimalValue(),
                            shuffleGroup, s);
                }
                shuffleGroup++;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "策略卷快照生成失败");
        }
    }

    private void insertSnapshotQuestion(Long snapshotId, int seq, BigDecimal score,
                                        int shuffleGroup, QuestionSnapshot s) {
        PaperQuestion pq = new PaperQuestion();
        pq.setSnapshotId(snapshotId);
        pq.setSeq(seq);
        pq.setScore(score);
        pq.setShuffleGroup(shuffleGroup);
        pq.setQuestionSnapshot(encrypt(toSnapshotJson(s)));
        paperQuestionMapper.insert(pq);
    }

    private List<PaperDetailVO.PreviewQuestion> snapshotQuestions(Long snapshotId, boolean withAnswer) {
        return paperQuestionMapper.selectList(Wrappers.lambdaQuery(PaperQuestion.class)
                        .eq(PaperQuestion::getSnapshotId, snapshotId)
                        .orderByAsc(PaperQuestion::getSeq))
                .stream().map(pq -> {
                    QuestionSnapshot s = fromSnapshotJson(decrypt(pq.getQuestionSnapshot()));
                    return new PaperDetailVO.PreviewQuestion(pq.getSeq(), s.type(), s.stem(), s.options(),
                            withAnswer ? s.answer() : null, withAnswer ? s.analysis() : null,
                            pq.getScore(), pq.getShuffleGroup());
                }).toList();
    }

    // ---------- 辅助 ----------

    /** 校验策略蓝图:slots 非空、题型合法、数量/分值为正。 */
    private String validateBlueprint(String blueprint) {
        try {
            JsonNode node = objectMapper.readTree(blueprint);
            JsonNode slots = node.path("slots");
            if (!slots.isArray() || slots.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "蓝图必须包含非空 slots 数组");
            }
            for (JsonNode slot : slots) {
                if (!List.of("single", "multiple", "judge", "fill", "subjective", "case", "operation")
                        .contains(slot.path("type").asText())) {
                    throw new BusinessException(ErrorCode.VALIDATE_FAILED, "蓝图含非法题型: " + slot.path("type").asText());
                }
                if (slot.path("count").asInt() <= 0 || slot.path("score").decimalValue().signum() <= 0) {
                    throw new BusinessException(ErrorCode.VALIDATE_FAILED, "蓝图 count/score 必须为正数");
                }
            }
            return blueprint;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "蓝图 JSON 格式不正确");
        }
    }

    private BigDecimal blueprintTotal(String blueprint) {
        try {
            JsonNode node = objectMapper.readTree(blueprint);
            BigDecimal total = BigDecimal.ZERO;
            for (JsonNode slot : node.path("slots")) {
                total = total.add(slot.path("score").decimalValue()
                        .multiply(BigDecimal.valueOf(slot.path("count").asInt())));
            }
            return total;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 整卷内容(未加密 JSON):固定卷含选题列表,策略卷含蓝图。 */
    private String buildSnapshotContent(Paper paper) {
        try {
            if ("fixed".equals(paper.getPaperType())) {
                return paper.getBlueprint() == null ? "{}" : paper.getBlueprint();
            }
            return "{\"blueprint\":" + paper.getBlueprint() + "}";
        } catch (Exception e) {
            return "{}";
        }
    }

    private String toSnapshotJson(QuestionSnapshot s) {
        try {
            return objectMapper.writeValueAsString(s);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目快照序列化失败");
        }
    }

    private QuestionSnapshot fromSnapshotJson(String json) {
        try {
            return objectMapper.readValue(json, QuestionSnapshot.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目快照反序列化失败");
        }
    }

    private String encrypt(String plain) {
        try {
            return AesUtil.encrypt(plain);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "快照加密失败");
        }
    }

    private String decrypt(String cipher) {
        try {
            return AesUtil.decrypt(cipher);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "快照解密失败");
        }
    }

    private String serializeFixedQuestions(List<PaperReq.QuestionItem> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "固定组卷必须至少选择一题");
        }
        try {
            return objectMapper.writeValueAsString(Map.of("questions", questions));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "选题列表序列化失败");
        }
    }

    private Paper require(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        return paper;
    }

    private PaperVO toVO(Paper paper) {
        int questionCount = 0;
        if ("published".equals(paper.getStatus())) {
            PaperSnapshot snapshot = snapshotMapper.selectOne(Wrappers.lambdaQuery(PaperSnapshot.class)
                    .eq(PaperSnapshot::getPaperId, paper.getId()).orderByDesc(PaperSnapshot::getId).last("LIMIT 1"));
            if (snapshot != null) {
                questionCount = Math.toIntExact(paperQuestionMapper.selectCount(Wrappers.lambdaQuery(PaperQuestion.class)
                        .eq(PaperQuestion::getSnapshotId, snapshot.getId())));
            }
        }
        return new PaperVO(paper.getId(), paper.getName(), paper.getSubjectId(), null,
                paper.getTotalScore(), paper.getPassScore(), paper.getDurationMin(),
                paper.getPaperType(), paper.getStatus(), questionCount, paper.getCreateTime());
    }

    /** 创建请求:固定卷传 questions,策略卷传 blueprint。 */
    public record PaperReq(String name, Long subjectId, BigDecimal passScore, Integer durationMin,
                           String paperType, String blueprint, List<QuestionItem> questions) {

        public record QuestionItem(Long questionId, BigDecimal score) {
        }
    }
}
