package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 作答明细只读视图(共享库,判分读取)。
 */
@Data
@TableName("answer_detail")
public class AnswerDetailView {

    private Long id;
    private Long sessionId;
    private Integer questionSeq;
    private String answer;
    private BigDecimal score;
    private String scoreStatus;
    private Integer version;
}
