package com.examflow.paper.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
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
 * 组卷接口(骨架,见 PRD FR-PAPER)。
 * 生产化 TODO:
 * 1. 组卷蓝图(题型/题量/分值/知识点/难度配比)JSON 校验;
 * 2. 发布时生成不可变试卷快照(paper_snapshot,题目内容快照而非引用);
 * 3. 可复现抽题:seed = SHA256(snapshotId + slotId + candidateId),审计可重放(TDD §6.3)。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
@Tag(name = "组卷服务")
public class PaperController {

    @GetMapping
    @Operation(summary = "分页查询试卷")
    public Result<PageResult<Object>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping
    @Operation(summary = "创建试卷(固定组卷或策略组卷)")
    @AuditLog(module = "paper", action = "创建试卷")
    public Result<Void> create(@RequestBody PaperReq req) {
        log.info("创建试卷: name={}, paperType={}", req.name(), req.paperType());
        // TODO: 策略组卷蓝图校验;固定组卷逐题校验
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "试卷预览(含答案模式)")
    public Result<Object> preview(@PathVariable Long id, @RequestParam boolean withAnswer) {
        // TODO: 权限校验(仅组卷人与审批人),题目内容按快照渲染
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "审批通过并发布(生成不可变快照)")
    @AuditLog(module = "paper", action = "试卷发布")
    public Result<Void> publish(@PathVariable Long id) {
        // TODO: 生成快照 + 校验总分/题量完整性 + Redis 预热
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record PaperReq(@NotBlank String name, @NotNull Long subjectId,
                           String paperType, Object blueprint, Integer durationMin) {
    }
}
