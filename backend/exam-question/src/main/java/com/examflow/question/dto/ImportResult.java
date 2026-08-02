package com.examflow.question.dto;

import java.util.List;

/**
 * 批量导入结果:逐行报告(PRD FR-QB-05)。
 */
public record ImportResult(int total, int success, int fail, List<RowError> errors) {

    public record RowError(int row, String message) {
    }
}
