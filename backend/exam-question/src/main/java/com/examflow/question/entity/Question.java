package com.examflow.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目表(question,见 TDD §4.2.2)。
 * 题型:single/multiple/judge/fill/subjective/case/operation。
 * 答案与解析生产环境按 AesUtil 加密存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {

    /** 题型 */
    private String type;

    /** 题干(富文本) */
    private String stem;

    /** 选项(JSON,选择题) */
    private String options;

    /** 正确答案(加密) */
    private String answer;

    /** 答案解析(加密) */
    private String analysis;

    /** 难度 1-5 */
    private Integer difficulty;

    /** 科目 ID */
    private Long subjectId;

    /** 状态:草稿/待审/已审/发布/停用 */
    private String status;

    /** 题目来源 */
    private String source;

    /** 出题人(审题流程校验出题人≠审题人,并支持归属校验) */
    private Long creatorId;
}
