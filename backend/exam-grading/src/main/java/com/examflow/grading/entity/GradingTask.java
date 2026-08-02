package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 主观题评阅任务(grading_task):一轮主观题一个任务,双评/仲裁轮次流转。
 * round: first → second → arbitration;status: pending/grading/graded/arbitrated。
 */
@Data
@TableName("grading_task")
public class GradingTask {

    private Long id;
    private Long sessionId;
    private Integer questionSeq;

    /** 各轮次阅卷员 ID(JSON,如 [1,2] 或 [1,2,3]) */
    private String graderIds;

    /** first/second/arbitration */
    private String round;

    /** pending/grading/graded/arbitrated */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
