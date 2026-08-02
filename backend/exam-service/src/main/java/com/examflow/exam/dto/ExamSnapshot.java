package com.examflow.exam.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考试快照(paper-service 内部接口返回)。
 */
public record ExamSnapshot(Long paperId, Integer durationMin, BigDecimal totalScore,
                           BigDecimal passScore, String blueprint, List<ExamQuestion> questions) {

    public record ExamQuestion(Long pqId, String type, String stem, String options,
                               String answer, BigDecimal score, Integer seq, Integer shuffleGroup) {
    }
}
