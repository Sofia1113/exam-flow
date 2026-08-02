package com.examflow.paper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 试卷(paper,见 TDD §4.2.2)。
 * 状态机:draft → pending → approved → published → archived;
 * published 时生成不可变快照(paper_snapshot)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("paper")
public class Paper extends BaseEntity {

    private String name;

    private Long subjectId;

    private java.math.BigDecimal totalScore;

    private java.math.BigDecimal passScore;

    /** 考试时长(分钟) */
    private Integer durationMin;

    /** fixed=固定组卷 / strategy=策略组卷 */
    private String paperType;

    /** 组卷蓝图(JSON,策略卷) */
    private String blueprint;

    /** draft/pending/approved/published/archived */
    private String status;

    private Long approverId;

    private LocalDateTime approveTime;

    private String approveOpinion;
}
