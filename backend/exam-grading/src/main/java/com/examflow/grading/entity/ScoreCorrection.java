package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 成绩更正/申诉记录(score_correction):type=appeal(公示期异议)/correct(发布后更正)。
 * 流程:申请 → 复核 → 审批 → 应用,全程留痕。
 */
@Data
@TableName("score_correction")
public class ScoreCorrection {

    private Long id;
    private Long scoreId;
    private BigDecimal fromValue;
    private BigDecimal toValue;
    private String reason;
    private Long applicant;
    private Long approver;
    private LocalDateTime approveTime;

    /** appeal/correct */
    private String type;

    /** pending/appealed/approved/rejected */
    private String status;

    private LocalDateTime createTime;
}
