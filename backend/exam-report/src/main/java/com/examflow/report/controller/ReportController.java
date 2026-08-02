package com.examflow.report.controller;

import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表接口(骨架,见 PRD FR-REPORT)。
 * 生产化 TODO:
 * 1. 数据权限:明细级报表仅授权组织范围(SQL 层拦截);
 * 2. 大报表异步生成(XXL-JOB/线程池),下载走 OSS 预签名;
 * 3. 试题分析(通过率/区分度/知识点掌握度)可选 ES 聚合。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "报表服务")
public class ReportController {

    @GetMapping("/plans/{planId}/overview")
    @Operation(summary = "考次总览:报名/实考/缺考/通过率/分数分布")
    public Result<Object> overview(@PathVariable @NotNull Long planId) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/plans/{planId}/questions")
    @Operation(summary = "试题分析")
    public Result<Object> questionAnalysis(@PathVariable Long planId) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/plans/{planId}/export")
    @Operation(summary = "导出考试分析报告(PDF/Word)")
    public Result<Object> export(@PathVariable Long planId) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }
}
