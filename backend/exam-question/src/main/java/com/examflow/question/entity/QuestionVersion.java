package com.examflow.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 题目版本历史(question_version):修改留痕,审计可追溯。
 */
@Data
@TableName("question_version")
public class QuestionVersion {

    private Long id;
    private Long questionId;

    /** 变更前内容(JSON) */
    private String contentSnapshot;

    private Long operatorId;

    /** create/update/audit/disable */
    private String operateType;

    private LocalDateTime createTime;
}
