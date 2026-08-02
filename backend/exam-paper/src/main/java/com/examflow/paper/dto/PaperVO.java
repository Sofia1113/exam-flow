package com.examflow.paper.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷视图(列表)。
 */
public record PaperVO(Long id, String name, Long subjectId, String subjectName,
                      BigDecimal totalScore, BigDecimal passScore, Integer durationMin,
                      String paperType, String status, Integer questionCount,
                      LocalDateTime createTime) {
}
