package com.examflow.grading.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.context.UserContext;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.Result;
import com.examflow.grading.dto.GradingTaskVO;
import com.examflow.grading.service.ReviewService;
import com.examflow.grading.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阅卷与成绩接口(FR-GRADE/FR-SCORE)。
 */
@RestController
@RequestMapping("/api/v1/grading")
@RequiredArgsConstructor
@Tag(name = "阅卷成绩服务")
public class GradingController {

    private final ReviewService reviewService;
    private final ScoreService scoreService;

    // ---------- 评阅 ----------

    @PostMapping("/assign")
    @Operation(summary = "分派评阅任务(按考次,至少 2 名阅卷员,双评)")
    @AuditLog(module = "grading", action = "评阅任务分派")
    public Result<Integer> assign(@RequestBody AssignReq req) {
        return Result.ok(reviewService.assignTasks(req.planId(), req.graderIds()));
    }

    @GetMapping("/tasks")
    @Operation(summary = "我的可评任务(脱敏)")
    public Result<List<GradingTaskVO>> myTasks() {
        return Result.ok(reviewService.myTasks(UserContext.requireUserId()));
    }

    @PostMapping("/tasks/{taskId}/score")
    @Operation(summary = "提交评分(双评/仲裁流转)")
    @AuditLog(module = "grading", action = "提交评分")
    public Result<Void> score(@PathVariable Long taskId, @RequestBody ScoreReq req) {
        reviewService.submitScore(taskId, UserContext.requireUserId(), req.score(), req.comment());
        return Result.ok();
    }

    @GetMapping("/progress")
    @Operation(summary = "评阅进度(按考次)")
    public Result<Map<String, Object>> progress(@RequestParam Long planId) {
        return Result.ok(reviewService.progress(planId));
    }

    // ---------- 成绩 ----------

    @PostMapping("/scores/publish")
    @Operation(summary = "成绩发布(进入公示期)")
    @AuditLog(module = "grading", action = "成绩发布")
    public Result<Integer> publish(@RequestBody PublishReq req) {
        return Result.ok(scoreService.publish(req.planId(), req.publicityDays() == null ? 3 : req.publicityDays()));
    }

    @GetMapping("/scores/my")
    @Operation(summary = "我的成绩(考生)")
    public Result<List<Map<String, Object>>> myScores() {
        return Result.ok(scoreService.myScores(UserContext.requireUserId()));
    }

    @PostMapping("/scores/{sessionId}/appeal")
    @Operation(summary = "公示期成绩申诉")
    @AuditLog(module = "grading", action = "成绩申诉")
    public Result<Long> appeal(@PathVariable Long sessionId, @RequestBody AppealReq req) {
        return Result.ok(scoreService.appeal(sessionId, UserContext.requireUserId(), req.reason()));
    }

    @PostMapping("/scores/{sessionId}/correct")
    @Operation(summary = "成绩更正申请(审批留痕)")
    @AuditLog(module = "grading", action = "成绩更正申请")
    public Result<Long> correct(@PathVariable Long sessionId, @RequestBody CorrectReq req) {
        return Result.ok(scoreService.correct(sessionId, req.toValue(), req.reason()));
    }

    @PostMapping("/corrections/{id}/approve")
    @Operation(summary = "更正/申诉审批(通过则生效)")
    @AuditLog(module = "grading", action = "成绩更正审批")
    public Result<Void> approve(@PathVariable Long id, @RequestParam boolean pass,
                                @RequestParam(required = false) String opinion) {
        scoreService.approve(id, pass, opinion, UserContext.requireUserId());
        return Result.ok();
    }

    @GetMapping("/scores/export")
    @Operation(summary = "成绩导出(按考次)")
    public void export(@RequestParam Long planId, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=scores.xlsx");
        scoreService.exportScores(response.getOutputStream(), planId);
    }

    public record AssignReq(@NotNull Long planId, @NotNull List<Long> graderIds) {
    }

    public record ScoreReq(@NotNull BigDecimal score, String comment) {
    }

    public record PublishReq(@NotNull Long planId, Integer publicityDays) {
    }

    public record AppealReq(@NotNull String reason) {
    }

    public record CorrectReq(@NotNull BigDecimal toValue, @NotNull String reason) {
    }
}
