package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评分记录(grading_record):双评/仲裁每次评分一条。
 */
@Data
@TableName("grading_record")
public class GradingRecord {

    private Long id;
    private Long taskId;
    private Long graderId;
    private BigDecimal score;
    private String comment;
    private LocalDateTime submitTime;
}
