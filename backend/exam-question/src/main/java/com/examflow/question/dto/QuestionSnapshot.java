package com.examflow.question.dto;

import java.util.List;

/**
 * 题目内容快照(内部接口返回明文,paper 服务加密存储到试卷快照)。
 */
public record QuestionSnapshot(Long questionId, String type, String stem, String options,
                               String answer, String analysis, Integer difficulty,
                               List<String> knowledges) {
}
