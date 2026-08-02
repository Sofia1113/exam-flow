package com.examflow.registration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报名记录(exam_registration):uk(plan_id,user_id) 防重复,名额靠唯一索引兜底。
 * 状态:pending(待审)/approved(已通过)/rejected(已驳回)/withdrawn(已退考)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_registration")
public class ExamRegistration extends BaseEntity {

    private Long planId;

    private Long userId;

    /** 场次(排考后回填) */
    private Long slotId;

    /** 机位号(排考后分配) */
    private String seatNo;

    private String status;

    private Long auditBy;

    private String auditOpinion;

    private LocalDateTime auditTime;

    /** 准考证号(审核通过后生成) */
    private String ticketNo;

    /** 1=候补 */
    private Integer waitlist;
}
