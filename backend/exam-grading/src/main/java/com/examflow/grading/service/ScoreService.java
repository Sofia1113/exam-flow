package com.examflow.grading.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.grading.client.PaperServiceClient;
import com.examflow.grading.client.UserServiceClient;
import com.examflow.grading.dto.ExamSnapshot;
import com.examflow.grading.dto.UserInfo;
import com.examflow.grading.entity.AnswerDetailView;
import com.examflow.grading.entity.ExamSessionView;
import com.examflow.grading.entity.RegistrationView;
import com.examflow.grading.entity.ScoreCorrection;
import com.examflow.grading.entity.ScoreRecord;
import com.examflow.grading.mapper.AnswerDetailViewMapper;
import com.examflow.grading.mapper.ExamSessionViewMapper;
import com.examflow.grading.mapper.GradingTaskMapper;
import com.examflow.grading.mapper.RegistrationViewMapper;
import com.examflow.grading.mapper.ScoreCorrectionMapper;
import com.examflow.grading.mapper.ScoreRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成绩服务(FR-SCORE):会话结算、发布/公示期、申诉、更正流程、成绩导出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private static final Set<String> SUBJECTIVE_TYPES = Set.of("subjective", "case", "operation");

    private final ScoreRecordMapper scoreRecordMapper;
    private final ExamSessionViewMapper sessionMapper;
    private final AnswerDetailViewMapper answerMapper;
    private final RegistrationViewMapper registrationMapper;
    private final GradingTaskMapper taskMapper;
    private final ScoreCorrectionMapper correctionMapper;
    private final com.examflow.grading.mapper.ExamPlanViewMapper planMapper;
    private final PaperServiceClient paperClient;
    private final UserServiceClient userClient;
    private final com.examflow.grading.client.MessageServiceClient messageClient;

    /** 会话全部主观题评完 → 核算主观分/总分/及格判定(幂等)。 */
    @Transactional
    public void settleIfComplete(Long sessionId) {
        long remaining = taskMapper.selectCount(Wrappers.lambdaQuery(
                        com.examflow.grading.entity.GradingTask.class)
                .eq(com.examflow.grading.entity.GradingTask::getSessionId, sessionId)
                .ne(com.examflow.grading.entity.GradingTask::getStatus, "graded"));
        if (remaining > 0) {
            return;
        }
        ExamSessionView session = sessionMapper.selectById(sessionId);
        ExamSnapshot snap = paperClient.examSnapshot(session.getPaperSnapshotId(), true);
        List<Integer> subSeqs = snap.questions().stream()
                .filter(q -> SUBJECTIVE_TYPES.contains(q.type()))
                .map(ExamSnapshot.ExamQuestion::seq).toList();
        BigDecimal subjective = BigDecimal.ZERO;
        if (!subSeqs.isEmpty()) {
            List<AnswerDetailView> details = answerMapper.selectList(Wrappers.lambdaQuery(AnswerDetailView.class)
                    .eq(AnswerDetailView::getSessionId, sessionId)
                    .in(AnswerDetailView::getQuestionSeq, subSeqs));
            for (AnswerDetailView d : details) {
                if (d.getScore() != null) {
                    subjective = subjective.add(d.getScore());
                }
            }
        }
        ScoreRecord record = scoreRecordMapper.selectOne(Wrappers.lambdaQuery(ScoreRecord.class)
                .eq(ScoreRecord::getSessionId, sessionId));
        if (record == null) {
            return;
        }
        record.setSubjectiveScore(subjective);
        record.setTotalScore(record.getObjectiveScore().add(subjective));
        record.setPassFlag(record.getTotalScore().compareTo(snap.passScore()) >= 0 ? 1 : 0);
        scoreRecordMapper.updateById(record);
        log.info("会话成绩结算: session={}, 总分={}, 及格={}", sessionId, record.getTotalScore(), record.getPassFlag());
    }

    /** 成绩发布(按考次):进入公示期,公示天数参数化。 */
    @Transactional
    public int publish(Long planId, int publicityDays) {
        List<RegistrationView> regs = registrationMapper.selectList(Wrappers.lambdaQuery(RegistrationView.class)
                .eq(RegistrationView::getPlanId, planId)
                .eq(RegistrationView::getStatus, "approved"));
        int published = 0;
        for (RegistrationView reg : regs) {
            ExamSessionView session = sessionMapper.selectOne(Wrappers.lambdaQuery(ExamSessionView.class)
                    .eq(ExamSessionView::getRegistrationId, reg.getId()));
            if (session == null) {
                continue;
            }
            ScoreRecord record = scoreRecordMapper.selectOne(Wrappers.lambdaQuery(ScoreRecord.class)
                    .eq(ScoreRecord::getSessionId, session.getId()));
            if (record == null || "published".equals(record.getPublishStatus())) {
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            record.setPublishStatus("publicity");
            record.setPublicityStart(now);
            record.setPublicityEnd(now.plusDays(publicityDays));
            scoreRecordMapper.updateById(record);
            // 成绩公布通知(站内信;失败不影响发布)
            try {
                UserInfo user = userClient.getUser(reg.getUserId());
                com.examflow.grading.entity.ExamPlanView plan = planMapper.selectById(planId);
                messageClient.send(new com.examflow.grading.client.MessageServiceClient.SendReq(
                        "score_published", String.valueOf(reg.getUserId()), "site",
                        Map.of("name", user == null ? "考生" : user.name(),
                                "examName", plan == null ? "" : plan.getName())));
            } catch (Exception e) {
                log.warn("成绩公布通知失败(不影响发布): session={}", session.getId(), e);
            }
            published++;
        }
        log.info("成绩发布(公示期): plan={}, {} 人进入公示", planId, published);
        return published;
    }

    /** 考生我的成绩(公示/已发布可见)。 */
    public List<Map<String, Object>> myScores(Long userId) {
        List<RegistrationView> regs = registrationMapper.selectList(Wrappers.lambdaQuery(RegistrationView.class)
                .eq(RegistrationView::getUserId, userId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RegistrationView reg : regs) {
            ExamSessionView session = sessionMapper.selectOne(Wrappers.lambdaQuery(ExamSessionView.class)
                    .eq(ExamSessionView::getRegistrationId, reg.getId()));
            if (session == null) {
                continue;
            }
            ScoreRecord record = scoreRecordMapper.selectOne(Wrappers.lambdaQuery(ScoreRecord.class)
                    .eq(ScoreRecord::getSessionId, session.getId()));
            if (record == null || "unpublished".equals(record.getPublishStatus())) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", session.getId());
            row.put("totalScore", record.getTotalScore());
            row.put("objectiveScore", record.getObjectiveScore());
            row.put("subjectiveScore", record.getSubjectiveScore());
            row.put("passFlag", record.getPassFlag());
            row.put("publishStatus", record.getPublishStatus());
            row.put("publicityEnd", record.getPublicityEnd());
            result.add(row);
        }
        return result;
    }

    /** 公示期成绩异议申诉(FR-SCORE-03):仅公示期内且本人会话。 */
    @Transactional
    public Long appeal(Long sessionId, Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "申诉理由必填");
        }
        ExamSessionView session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        RegistrationView reg = registrationMapper.selectById(session.getRegistrationId());
        if (reg == null || !reg.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅本人可申诉本人成绩");
        }
        ScoreRecord record = scoreRecordMapper.selectOne(Wrappers.lambdaQuery(ScoreRecord.class)
                .eq(ScoreRecord::getSessionId, sessionId));
        if (record == null || !"publicity".equals(record.getPublishStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅公示期内可申诉");
        }
        ScoreCorrection correction = new ScoreCorrection();
        correction.setScoreId(record.getId());
        correction.setFromValue(record.getTotalScore());
        correction.setReason(reason);
        correction.setApplicant(userId);
        correction.setType("appeal");
        correction.setStatus("pending");
        correction.setCreateTime(LocalDateTime.now());
        correctionMapper.insert(correction);
        return correction.getId();
    }

    /** 成绩更正申请(FR-SCORE-07):发布后修正须走流程并留痕。 */
    @Transactional
    public Long correct(Long sessionId, BigDecimal toValue, String reason) {
        ScoreRecord record = scoreRecordMapper.selectOne(Wrappers.lambdaQuery(ScoreRecord.class)
                .eq(ScoreRecord::getSessionId, sessionId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成绩不存在");
        }
        ScoreCorrection correction = new ScoreCorrection();
        correction.setScoreId(record.getId());
        correction.setFromValue(record.getTotalScore());
        correction.setToValue(toValue);
        correction.setReason(reason);
        correction.setApplicant(com.examflow.common.context.UserContext.requireUserId());
        correction.setType("correct");
        correction.setStatus("pending");
        correction.setCreateTime(LocalDateTime.now());
        correctionMapper.insert(correction);
        return correction.getId();
    }

    /** 更正/申诉审批:通过则应用修正(留痕),驳回记录原因。 */
    @Transactional
    public void approve(Long correctionId, boolean pass, String opinion, Long approverId) {
        ScoreCorrection correction = correctionMapper.selectById(correctionId);
        if (correction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "申请不存在");
        }
        if (!"pending".equals(correction.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "申请已处理");
        }
        correction.setApprover(approverId);
        correction.setApproveTime(LocalDateTime.now());
        correction.setStatus(pass ? "approved" : "rejected");
        correctionMapper.updateById(correction);
        if (pass && correction.getToValue() != null) {
            ScoreRecord record = scoreRecordMapper.selectById(correction.getScoreId());
            if (record != null) {
                BigDecimal passScore = passScoreOf(record.getSessionId());
                record.setTotalScore(correction.getToValue());
                // 及格判定按试卷快照及格线,修复恒为合格的逻辑缺陷
                record.setPassFlag(passScore != null
                        && correction.getToValue().compareTo(passScore) >= 0 ? 1 : 0);
                scoreRecordMapper.updateById(record);
                log.info("成绩更正生效: score={}, {} → {}, 及格={}, 原因={}, 审批人={}",
                        record.getId(), correction.getFromValue(), correction.getToValue(),
                        record.getPassFlag(), correction.getReason(), approverId);
            }
        }
    }

    /** 试卷快照及格线(更正后重新判定合格)。 */
    private BigDecimal passScoreOf(Long sessionId) {
        ExamSessionView session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }
        try {
            return paperClient.examSnapshot(session.getPaperSnapshotId(), false).passScore();
        } catch (Exception e) {
            log.warn("获取及格线失败: session={}", sessionId, e);
            return null;
        }
    }
    }

    /** 成绩导出(按考次,加密 Excel 待 M5 落地,当前普通导出)。 */
    public void exportScores(java.io.OutputStream out, Long planId) {
        List<RegistrationView> regs = registrationMapper.selectList(Wrappers.lambdaQuery(RegistrationView.class)
                .eq(RegistrationView::getPlanId, planId)
                .eq(RegistrationView::getStatus, "approved"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RegistrationView reg : regs) {
            ExamSessionView session = sessionMapper.selectOne(Wrappers.lambdaQuery(ExamSessionView.class)
                    .eq(ExamSessionView::getRegistrationId, reg.getId()));
            if (session == null) {
                continue;
            }
            ScoreRecord record = scoreRecordMapper.selectOne(Wrappers.lambdaQuery(ScoreRecord.class)
                    .eq(ScoreRecord::getSessionId, session.getId()));
            if (record == null) {
                continue;
            }
            UserInfo user = userClient.getUser(reg.getUserId());
            Map<String, Object> row = new HashMap<>();
            row.put("姓名", user == null ? "" : user.name());
            row.put("账号", user == null ? "" : user.username());
            row.put("总分", record.getTotalScore());
            row.put("客观题", record.getObjectiveScore());
            row.put("主观题", record.getSubjectiveScore());
            row.put("是否合格", record.getPassFlag() == 1 ? "合格" : "不合格");
            row.put("状态", record.getPublishStatus());
            rows.add(row);
        }
        EasyExcel.write(out).sheet("成绩").doWrite(rows);
    }
}
