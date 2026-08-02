package com.examflow.paper.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.context.UserContext;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import com.examflow.paper.dto.PaperDetailVO;
import com.examflow.paper.dto.PaperVO;
import com.examflow.paper.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组卷接口(FR-PAPER):固定/策略组卷、状态机、预览、审批发布、快照。
 */
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
@Tag(name = "组卷服务")
public class PaperController {

    private final PaperService paperService;

    @GetMapping
    @Operation(summary = "试卷分页(按科目/类型/状态)")
    public Result<PageResult<PaperVO>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) Long subjectId,
                                            @RequestParam(required = false) String paperType,
                                            @RequestParam(required = false) String status) {
        return Result.ok(paperService.page(page, size, subjectId, paperType, status));
    }

    @PostMapping
    @Operation(summary = "创建试卷(固定组卷传 questions,策略组卷传 blueprint)")
    @AuditLog(module = "paper", action = "创建试卷")
    public Result<Long> create(@RequestBody PaperService.PaperReq req) {
        return Result.ok(paperService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新试卷(仅草稿/待审)")
    @AuditLog(module = "paper", action = "更新试卷")
    public Result<Void> update(@PathVariable Long id, @RequestBody PaperService.PaperReq req) {
        paperService.update(id, req);
        return Result.ok();
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "试卷预览/详情(withAnswer=true 返回解密答案)")
    public Result<PaperDetailVO> preview(@PathVariable Long id,
                                         @RequestParam(defaultValue = "false") boolean withAnswer) {
        return Result.ok(paperService.detail(id, withAnswer));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "送审(草稿→待审)")
    @AuditLog(module = "paper", action = "试卷送审")
    public Result<Void> submit(@PathVariable Long id) {
        paperService.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "审批(通过→已审,驳回→草稿)")
    @AuditLog(module = "paper", action = "试卷审批")
    public Result<Void> audit(@PathVariable Long id, @RequestParam boolean pass,
                              @RequestParam(required = false) String opinion) {
        paperService.audit(id, pass, opinion, UserContext.requireUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布:生成不可变快照(题目内容快照)")
    @AuditLog(module = "paper", action = "试卷发布")
    public Result<Void> publish(@PathVariable Long id) {
        paperService.publish(id);
        return Result.ok();
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档(历史卷)")
    @AuditLog(module = "paper", action = "试卷归档")
    public Result<Void> archive(@PathVariable Long id) {
        paperService.archive(id);
        return Result.ok();
    }
}
