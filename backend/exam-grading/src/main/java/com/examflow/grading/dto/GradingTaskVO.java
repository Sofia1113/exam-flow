package com.examflow.grading.dto;

import java.math.BigDecimal;

/**
 * 评阅任务视图(脱敏:不含考生姓名/单位/准考证等任何身份信息)。
 */
public record GradingTaskVO(Long taskId, Integer questionSeq, String type, String stem,
                            String options, BigDecimal fullScore, String keyPoints,
                            String studentAnswer, String round, String status) {
}
