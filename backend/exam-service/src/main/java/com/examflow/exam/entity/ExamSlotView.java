package com.examflow.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 场次只读视图(与 registration 服务共享表,考试时间窗校验用)。
 */
@Data
@TableName("exam_slot")
public class ExamSlotView {

    private Long id;
    private Long planId;
    private String slotName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
