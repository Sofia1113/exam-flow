package com.examflow.paper.dto;

import java.util.List;

/**
 * 题目内容快照(question-service 内部接口返回)。
 */
public record QuestionSnapshot(Long questionId, String type, String stem, String options,
                               String answer, String analysis, Integer difficulty,
                               List<String> knowledges) {
}
