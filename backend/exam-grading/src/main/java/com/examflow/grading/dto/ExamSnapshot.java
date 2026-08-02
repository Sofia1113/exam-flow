package com.examflow.grading.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考试快照(判分用,含标准答案)。
 */
public record ExamSnapshot(Long paperId, Integer durationMin, BigDecimal totalScore,
                           BigDecimal passScore, String blueprint, List<ExamQuestion> questions) {

    public record ExamQuestion(Long pqId, String type, String stem, String options,
                               String answer, BigDecimal score, Integer seq, Integer shuffleGroup) {
    }
}
