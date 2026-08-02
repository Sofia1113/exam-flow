package com.examflow.registration.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报名与排考接口(骨架,见 PRD FR-REG / FR-SCHED)。
 * 生产化 TODO:
 * 1. 报名条件规则引擎自动预审 + 人工审核双通道;
 * 2. 名额控制(满即止/候补名单),报名成功生成准考证号;
 * 3. 排考冲突检测(同一考生同时段唯一,跨考次同样检测);
 * 4. 场次开始/结束由 XXL-JOB 驱动状态迁移。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
@Tag(name = "报名排考服务")
public class RegistrationController {

    @GetMapping("/plans")
    @Operation(summary = "考试计划列表")
    public Result<PageResult<Object>> plans(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/plans")
    @Operation(summary = "创建考试计划(提交审批)")
    @AuditLog(module = "registration", action = "创建考试计划")
    public Result<Void> createPlan(@RequestBody PlanReq req) {
        log.info("创建考试计划: name={}", req.name());
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/apply")
    @Operation(summary = "考生报名")
    @AuditLog(module = "registration", action = "提交报名")
    public Result<Void> apply(@RequestBody ApplyReq req) {
        // TODO: 自动预审(组织范围/前置资格)+ 名额占位
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/apply/{id}/audit")
    @Operation(summary = "报名审核(通过/驳回)")
    @AuditLog(module = "registration", action = "报名审核")
    public Result<Void> audit(@PathVariable Long id, @RequestParam boolean pass,
                              @RequestParam(required = false) String opinion) {
        // TODO: 驳回释放名额;通过生成准考证号
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/ticket/{registrationId}")
    @Operation(summary = "查询/下载准考证")
    public Result<Object> ticket(@PathVariable Long registrationId) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record PlanReq(@NotNull String name, @NotNull Long subjectId, Long paperId,
                          String regStart, String regEnd, String examDate, Integer capacity) {
    }

    public record ApplyReq(@NotNull Long planId, @NotNull Long userId, Long slotId) {
    }
}
