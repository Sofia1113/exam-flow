package com.examflow.question.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目视图。
 * 列表接口不含 answer/analysis(敏感,仅详情返回,权限控制待方法级鉴权落地);
 * detail 返回明文答案供组卷人/审批人使用。
 */
public record QuestionVO(Long id, String type, String stem, String options,
                         String answer, String analysis, Integer difficulty,
                         Long subjectId, String subjectName, String status, String source,
                         List<String> tags, List<String> knowledges, LocalDateTime createTime) {
}
