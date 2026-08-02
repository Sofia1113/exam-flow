package com.examflow.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报名记录只读视图(与 registration 服务共享表)。
 */
@Data
@TableName("exam_registration")
public class ExamRegistration {

    private Long id;
    private Long planId;
    private Long userId;
    private Long slotId;
    private String seatNo;
    private String status;
    private String ticketNo;
    private LocalDateTime createTime;
}
