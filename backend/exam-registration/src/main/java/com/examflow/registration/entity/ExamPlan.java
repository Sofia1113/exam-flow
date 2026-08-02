package com.examflow.registration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考试计划(exam_plan):状态机 draft → pending → approved → running → closed。
 * condition_rule 为报名条件规则(JSON):{"orgIds":[...],"userTypes":[...]}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_plan")
public class ExamPlan extends BaseEntity {

    private String name;

    private Long subjectId;

    /** 试卷(审批通过后关联) */
    private Long paperId;

    private LocalDateTime regStart;

    private LocalDateTime regEnd;

    private LocalDateTime examDate;

    /** 名额上限,0=不限 */
    private Integer capacity;

    /** 报名条件规则(JSON) */
    private String conditionRule;

    /** draft/pending/approved/running/closed */
    private String status;

    private Long approverId;

    private LocalDateTime approveTime;

    private String approveOpinion;
}
