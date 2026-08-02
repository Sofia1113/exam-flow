package com.examflow.question.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.util.AesUtil;
import com.examflow.question.dto.QuestionSnapshot;
import com.examflow.question.entity.Question;
import com.examflow.question.entity.QuestionKnowledge;
import com.examflow.question.mapper.QuestionKnowledgeMapper;
import com.examflow.question.mapper.QuestionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库内部接口(/internal/** 不经网关,仅组卷等可信服务直连)。
 * 返回题目明文快照(含答案),paper 服务负责加密存储到试卷快照。
 */
@Slf4j
@RestController
@RequestMapping("/internal/questions")
@RequiredArgsConstructor
public class InternalQuestionController {

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper knowledgeMapper;

    /** 按 ID 批量取题目快照(组卷/快照用,顺序与传入 ids 一致)。 */
    @GetMapping("/batch")
    public List<QuestionSnapshot> batch(@RequestParam List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        var map = questionMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));
        return ids.stream().map(map::get).filter(java.util.Objects::nonNull).map(this::toSnapshot).toList();
    }

    /** 候选题池:按科目/题型/难度/知识点过滤已发布题目(策略组卷用)。 */
    @GetMapping("/pool")
    public List<QuestionSnapshot> pool(@RequestParam Long subjectId,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) Integer difficulty,
                                       @RequestParam(required = false) String knowledge) {
        var wrapper = Wrappers.lambdaQuery(Question.class)
                .eq(Question::getSubjectId, subjectId)
                .eq(Question::getStatus, "published");
        if (type != null && !type.isBlank()) {
            wrapper.eq(Question::getType, type);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (knowledge != null && !knowledge.isBlank()) {
            var qids = knowledgeMapper.selectList(Wrappers.lambdaQuery(QuestionKnowledge.class)
                            .eq(QuestionKnowledge::getKnowledgeName, knowledge))
                    .stream().map(QuestionKnowledge::getQuestionId).toList();
            if (qids.isEmpty()) {
                return List.of();
            }
            wrapper.in(Question::getId, qids);
        }
        return questionMapper.selectList(wrapper).stream().map(this::toSnapshot).toList();
    }

    private QuestionSnapshot toSnapshot(Question q) {
        List<String> knowledges = knowledgeMapper.selectList(Wrappers.lambdaQuery(QuestionKnowledge.class)
                        .eq(QuestionKnowledge::getQuestionId, q.getId()))
                .stream().map(QuestionKnowledge::getKnowledgeName).toList();
        try {
            return new QuestionSnapshot(q.getId(), q.getType(), q.getStem(), q.getOptions(),
                    AesUtil.decrypt(q.getAnswer()), AesUtil.decrypt(q.getAnalysis()),
                    q.getDifficulty(), knowledges);
        } catch (Exception e) {
            log.error("题目快照解密失败: id={}", q.getId(), e);
            return new QuestionSnapshot(q.getId(), q.getType(), q.getStem(), q.getOptions(),
                    null, null, q.getDifficulty(), knowledges);
        }
    }
}
