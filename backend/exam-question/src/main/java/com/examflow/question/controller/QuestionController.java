package com.examflow.question.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import com.examflow.question.dto.QuestionVO;
import com.examflow.question.entity.SysSubject;
import com.examflow.question.mapper.SubjectMapper;
import com.examflow.question.service.QuestionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库接口(FR-QB-01~03)。
 * 送审/审题(FR-QB-04)与批量导入(FR-QB-05)为后续任务,保持占位。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "题库服务")
public class QuestionController {

    private final QuestionService questionService;
    private final SubjectMapper subjectMapper;

    @GetMapping
    @Operation(summary = "题目分页(按题型/科目/难度/状态/关键词)")
    public Result<PageResult<QuestionVO>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Long subjectId,
                                               @RequestParam(required = false) Integer difficulty,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword) {
        return Result.ok(questionService.page(page, size, type, subjectId, difficulty, status, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "题目详情(含解密答案,仅组卷/审题场景)")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.ok(questionService.detail(id));
    }

    @PostMapping
    @Operation(summary = "创建题目(状态=草稿,按题型结构校验)")
    @AuditLog(module = "question", action = "创建题目")
    public Result<Long> create(@RequestBody QuestionService.QuestionReq req) {
        return Result.ok(questionService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新题目(已发布禁止修改)")
    @AuditLog(module = "question", action = "更新题目")
    public Result<Void> update(@PathVariable Long id, @RequestBody QuestionService.QuestionReq req) {
        questionService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除题目(逻辑删除)")
    @AuditLog(module = "question", action = "删除题目")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }

    @GetMapping("/subjects")
    @Operation(summary = "科目列表(下拉)")
    public Result<List<SysSubject>> subjects() {
        return Result.ok(subjectMapper.selectList(Wrappers.lambdaQuery(SysSubject.class)
                .eq(SysSubject::getStatus, "enabled").orderByAsc(SysSubject::getId)));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "送审(M1-007 实现)")
    public Result<Void> submit(@PathVariable Long id) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "审题(M1-007 实现)")
    public Result<Void> audit(@PathVariable Long id, @RequestParam boolean pass,
                              @RequestParam(required = false) String opinion) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入(M1-008 实现)")
    public Result<Object> importExcel(@RequestBody ImportReq req) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record ImportReq(@NotBlank String fileUrl, Long subjectId) {
    }
}
