package com.examflow.grading.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.util.AesUtil;
import com.examflow.grading.client.PaperServiceClient;
import com.examflow.grading.dto.ExamSnapshot;
import com.examflow.grading.entity.AnswerDetailView;
import com.examflow.grading.entity.ExamSessionView;
import com.examflow.grading.entity.ScoreRecord;
import com.examflow.grading.mapper.AnswerDetailViewMapper;
import com.examflow.grading.mapper.ExamSessionViewMapper;
import com.examflow.grading.mapper.ScoreRecordMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客观题自动判分与成绩核算(FR-GRADE-01/07)。
 * 判分只依赖试卷快照(标准答案),幂等可重放;主观题跳过,由人工评阅(M3)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

    /** 多选题部分分比例(漏选得 50%),后续参数化到 sys_param。 */
    private static final BigDecimal MULTIPLE_PARTIAL_RATIO = BigDecimal.valueOf(0.5);

    private final ExamSessionViewMapper sessionViewMapper;
    private final AnswerDetailViewMapper answerViewMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final PaperServiceClient paperClient;
    private final ObjectMapper objectMapper;

    /** 判分:客观题评分 + 总分核算 + 状态迁移(幂等,重复调用安全)。 */
    @Transactional
    public void grade(Long sessionId) {
        ExamSessionView session = sessionViewMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        // 幂等:已出分不重复判
        if ("GRADED".equals(session.getStatus())) {
            log.info("会话已判分,跳过: session={}", sessionId);
            return;
        }
        ExamSnapshot snap = paperClient.examSnapshot(session.getPaperSnapshotId(), true);
        Map<Integer, ExamSnapshot.ExamQuestion> bySeq = snap.questions().stream()
                .collect(Collectors.toMap(ExamSnapshot.ExamQuestion::seq, q -> q));

        List<AnswerDetailView> details = answerViewMapper.selectList(
                Wrappers.lambdaQuery(AnswerDetailView.class)
                        .eq(AnswerDetailView::getSessionId, sessionId));

        BigDecimal objective = BigDecimal.ZERO;
        for (AnswerDetailView detail : details) {
            ExamSnapshot.ExamQuestion q = bySeq.get(detail.getQuestionSeq());
            if (q == null) {
                continue;
            }
            BigDecimal score = gradeOne(q.type(), q.score(), q.answer(), decrypt(detail.getAnswer()));
            if (score == null) {
                continue; // 主观题,人工评阅
            }
            detail.setScore(score);
            detail.setScoreStatus("graded");
            answerViewMapper.updateById(detail);
            objective = objective.add(score);
        }

        // 成绩核算:总分 = 客观分 + 主观分(人工评阅后回填)
        ScoreRecord record = scoreRecordMapper.selectOne(Wrappers.lambdaQuery(ScoreRecord.class)
                .eq(ScoreRecord::getSessionId, sessionId));
        if (record == null) {
            record = new ScoreRecord();
            record.setSessionId(sessionId);
            record.setObjectiveScore(objective);
            record.setSubjectiveScore(BigDecimal.ZERO);
            record.setTotalScore(objective);
            record.setPassFlag(0);
            record.setPublishStatus("unpublished");
            record.setCreateTime(LocalDateTime.now());
            scoreRecordMapper.insert(record);
        } else {
            record.setObjectiveScore(objective);
            record.setTotalScore(objective.add(record.getSubjectiveScore()));
            scoreRecordMapper.updateById(record);
        }
        sessionViewMapper.update(null, Wrappers.lambdaUpdate(ExamSessionView.class)
                .eq(ExamSessionView::getId, sessionId)
                .set(ExamSessionView::getStatus, "GRADED"));
        log.info("客观题判分完成: session={}, 客观分={}", sessionId, objective);
    }

    /** 单题判分;主观题返回 null(跳过)。判分规则与快照绑定,重放结果一致。 */
    private BigDecimal gradeOne(String type, BigDecimal fullScore, String correct, String studentAnswer) {
        if (correct == null || studentAnswer == null) {
            return BigDecimal.ZERO;
        }
        String student = studentAnswer.trim();
        switch (type) {
            case "single", "judge" -> {
                // 精确匹配(判分规则固化为快照,后续可参数化)
                return correct.trim().equalsIgnoreCase(student) ? fullScore : BigDecimal.ZERO;
            }
            case "multiple" -> {
                List<String> c = parseList(correct);
                List<String> s = parseList(student);
                if (s.isEmpty() || s.size() != c.size() || !s.containsAll(c)) {
                    return BigDecimal.ZERO;
                }
                return fullScore;
            }
            case "fill" -> {
                List<String> c = parseList(correct);
                List<String> s = parseList(student);
                if (c.isEmpty() || c.size() != s.size()) {
                    return BigDecimal.ZERO;
                }
                BigDecimal per = fullScore.divide(BigDecimal.valueOf(c.size()), 2, RoundingMode.HALF_UP);
                BigDecimal sum = BigDecimal.ZERO;
                for (int i = 0; i < c.size(); i++) {
                    if (c.get(i).trim().equalsIgnoreCase(s.get(i).trim())) {
                        sum = sum.add(per);
                    }
                }
                return sum;
            }
            default -> {
                return null; // subjective/case/operation:人工评阅
            }
        }
    }

    private List<String> parseList(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(n -> result.add(n.asText()));
            } else {
                result.add(node.asText());
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String decrypt(String cipher) {
        try {
            return AesUtil.decrypt(cipher);
        } catch (Exception e) {
            log.warn("作答解密失败", e);
            return null;
        }
    }
}
