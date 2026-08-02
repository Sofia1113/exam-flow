package com.examflow.question.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
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
 * 题库接口(骨架,见 PRD FR-QB)。
 * 生产化 TODO:
 * 1. 审题流:出题人与审题人不可为同一人,发布后修改需重新送审(状态机);
 * 2. 批量导入 Excel/Word 模板,导入错误逐行报告;
 * 3. 题目检索(ES,可后接入);修改留痕 question_version。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "题库服务")
public class QuestionController {

    @GetMapping
    @Operation(summary = "分页查询题目(按科目/题型/难度/状态)")
    public Result<PageResult<Object>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "题目详情")
    public Result<Object> detail(@PathVariable Long id) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping
    @Operation(summary = "创建题目(状态=草稿)")
    @AuditLog(module = "question", action = "创建题目")
    public Result<Void> create(@RequestBody QuestionReq req) {
        log.info("创建题目: type={}", req.type());
        // TODO: 按题型校验结构完整性(选择题必含选项与答案等)
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "送审")
    @AuditLog(module = "question", action = "题目送审")
    public Result<Void> submit(@PathVariable Long id) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "审题(通过/驳回)")
    @AuditLog(module = "question", action = "题目审核")
    public Result<Void> audit(@PathVariable Long id, @RequestParam boolean pass,
                              @RequestParam(required = false) String opinion) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/import")
    @Operation(summary = "Excel 批量导入")
    public Result<Object> importExcel(@RequestBody ImportReq req) {
        // TODO: 逐行校验与报告,返回成功/失败行数
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record QuestionReq(@NotBlank String type, @NotBlank String stem, String options,
                              String answer, String analysis, Integer difficulty,
                              Long subjectId, String source) {
    }

    public record ImportReq(@NotBlank String fileUrl, Long subjectId) {
    }
}
