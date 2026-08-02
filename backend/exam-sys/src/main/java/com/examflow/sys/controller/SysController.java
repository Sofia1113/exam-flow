package com.examflow.sys.controller;

import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统管理接口(骨架,见 PRD FR-SYS)。
 * 生产化 TODO:
 * 1. 字典与全局参数(安全策略/防作弊阈值/公示期天数)统一缓存,变更发布通知;
 * 2. 审计日志检索/导出,仅审计员与系统管理员可见;
 * 3. 备份策略配置与备份任务(XXL-JOB)。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sys")
@RequiredArgsConstructor
@Tag(name = "系统服务")
public class SysController {

    @GetMapping("/dicts")
    @Operation(summary = "字典列表")
    public Result<PageResult<Object>> dicts(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/params")
    @Operation(summary = "全局参数")
    public Result<Object> params() {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PutMapping("/params")
    @Operation(summary = "更新全局参数")
    public Result<Void> updateParams(@RequestBody Object params) {
        // TODO: 参数变更留痕 + 缓存失效发布
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "审计日志检索(仅审计员/系统管理员)")
    public Result<PageResult<Object>> auditLogs(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long size) {
        // TODO: 角色校验 + 时间范围/操作人/模块过滤
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }
}
