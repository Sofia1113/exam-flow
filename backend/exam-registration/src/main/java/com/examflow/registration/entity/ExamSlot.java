package com.examflow.registration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考试场次(exam_slot):一个考次可分多场错峰。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_slot")
public class ExamSlot extends BaseEntity {

    private Long planId;

    private String slotName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer capacity;

    /** 已分配机位 */
    private Integer seatCount;

    /** 监考员 ID 列表(JSON) */
    private String proctorIds;
}
