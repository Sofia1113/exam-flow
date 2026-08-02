package com.examflow.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目标签(question_tag)。
 */
@Data
@TableName("question_tag")
public class QuestionTag {

    private Long id;
    private Long questionId;
    private String tagName;
}
