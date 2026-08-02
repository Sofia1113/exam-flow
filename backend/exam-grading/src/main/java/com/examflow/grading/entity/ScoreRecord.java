package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 成绩记录(score_record):uk(session_id)。
 */
@Data
@TableName("score_record")
public class ScoreRecord {

    private Long id;
    private Long sessionId;
    private BigDecimal totalScore;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private Integer passFlag;
    private String publishStatus;
    private LocalDateTime publicityStart;
    private LocalDateTime publicityEnd;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
