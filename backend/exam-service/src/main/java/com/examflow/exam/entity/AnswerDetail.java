package com.examflow.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 作答明细(answer_detail,分片表,分片键=session_id)。
 * answer 为加密 JSON;version 供 seq 对齐。
 */
@Data
@TableName("answer_detail")
public class AnswerDetail {

    private Long id;
    private Long sessionId;
    private Integer questionSeq;
    private String answer;
    private BigDecimal score;
    private String scoreStatus;
    private Integer version;
    private LocalDateTime updateTime;
}
