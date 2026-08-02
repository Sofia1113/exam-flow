package com.examflow.paper.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考试快照(考试服务进入考试/判分服务判分使用)。
 * questions 不含答案;判分场景通过 withAnswer=true 获取。
 */
public record ExamSnapshot(Long paperId, Integer durationMin, BigDecimal totalScore,
                           BigDecimal passScore, String blueprint, List<ExamQuestion> questions) {

    public record ExamQuestion(Long pqId, String type, String stem, String options,
                               String answer, BigDecimal score, Integer seq, Integer shuffleGroup) {
    }
}
