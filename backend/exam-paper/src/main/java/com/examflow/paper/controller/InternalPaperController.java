package com.examflow.paper.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.util.AesUtil;
import com.examflow.paper.dto.ExamSnapshot;
import com.examflow.paper.entity.Paper;
import com.examflow.paper.entity.PaperQuestion;
import com.examflow.paper.entity.PaperSnapshot;
import com.examflow.paper.mapper.PaperMapper;
import com.examflow.paper.mapper.PaperQuestionMapper;
import com.examflow.paper.mapper.PaperSnapshotMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组卷内部接口(/internal/** 不经网关,供考试/判分服务直连)。
 * 考试快照:已发布试卷的卷面题目(不含答案;判分场景 withAnswer=true)。
 */
@Slf4j
@RestController
@RequestMapping("/internal/papers")
@RequiredArgsConstructor
public class InternalPaperController {

    private final PaperMapper paperMapper;
    private final PaperSnapshotMapper snapshotMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ObjectMapper objectMapper;

    /** 考试快照:仅已发布试卷可考;withAnswer=true 供判分服务。 */
    @GetMapping("/{paperId}/exam-snapshot")
    public ExamSnapshot examSnapshot(@PathVariable Long paperId,
                                     @RequestParam(defaultValue = "false") boolean withAnswer) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        if (!"published".equals(paper.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "考试须使用已发布试卷");
        }
        PaperSnapshot snapshot = snapshotMapper.selectOne(Wrappers.lambdaQuery(PaperSnapshot.class)
                .eq(PaperSnapshot::getPaperId, paperId)
                .orderByDesc(PaperSnapshot::getId).last("LIMIT 1"));
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "试卷快照缺失");
        }
        List<ExamSnapshot.ExamQuestion> questions = paperQuestionMapper.selectList(
                        Wrappers.lambdaQuery(PaperQuestion.class)
                                .eq(PaperQuestion::getSnapshotId, snapshot.getId())
                                .orderByAsc(PaperQuestion::getSeq))
                .stream().map(pq -> toQuestion(pq, withAnswer)).toList();
        // 策略卷快照 content 含蓝图(抽题 count 依据)
        String blueprint = null;
        try {
            String content = AesUtil.decrypt(snapshot.getContent());
            blueprint = objectMapper.readTree(content).path("blueprint").toString();
        } catch (Exception e) {
            log.warn("快照蓝图解析失败: snapshot={}", snapshot.getId());
        }
        return new ExamSnapshot(paper.getId(), paper.getDurationMin(), paper.getTotalScore(),
                paper.getPassScore(), blueprint, questions);
    }

    private ExamSnapshot.ExamQuestion toQuestion(PaperQuestion pq, boolean withAnswer) {
        try {
            var node = objectMapper.readTree(AesUtil.decrypt(pq.getQuestionSnapshot()));
            String type = node.path("type").asText();
            String stem = node.path("stem").asText();
            String options = node.path("options").isNull() ? null : node.path("options").asText();
            String answer = null;
            if (withAnswer) {
                answer = node.path("answer").isNull() ? null : node.path("answer").asText();
            }
            return new ExamSnapshot.ExamQuestion(pq.getId(), type, stem, options, answer,
                    pq.getScore(), pq.getSeq(), pq.getShuffleGroup());
        } catch (Exception e) {
            log.error("快照题目解析失败: pq={}", pq.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "快照题目解析失败");
        }
    }
}
