package com.examflow.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目知识点(question_knowledge,组卷按知识点抽题)。
 */
@Data
@TableName("question_knowledge")
public class QuestionKnowledge {

    private Long id;
    private Long questionId;
    private String knowledgeName;
}
