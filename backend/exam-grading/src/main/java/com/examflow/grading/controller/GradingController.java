package com.examflow.grading.controller;

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
 * 阅卷与成绩接口(骨架,见 PRD FR-GRADE / FR-SCORE)。
 * 生产化 TODO:
 * 1. 消费"交卷完成"事件:客观题自动判分(仅依赖试卷快照,幂等可重放);
 * 2. 主观题双评:分差≤阈值取均值,>阈值三评仲裁;答卷脱敏(匿名答卷 ID);
 * 3. 成绩发布/公示期/申诉/更正流程(更正必须审批留痕);
 * 4. 证书生成(PDF + 防伪二维码)。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/grading")
@RequiredArgsConstructor
@Tag(name = "阅卷成绩服务")
public class GradingController {

    @GetMapping("/tasks")
    @Operation(summary = "评阅任务列表(按阅卷员)")
    public Result<PageResult<Object>> tasks(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "评阅界面:脱敏答卷 + 评分细则")
    public Result<Object> taskDetail(@PathVariable Long taskId) {
        // TODO: 隐藏姓名/单位/准考证号,侧栏展示采分点
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/tasks/{taskId}/score")
    @Operation(summary = "提交评分(双评/仲裁轮次)")
    @AuditLog(module = "grading", action = "提交评分")
    public Result<Void> score(@PathVariable Long taskId, @RequestBody ScoreReq req) {
        log.info("提交评分: task={}, score={}", taskId, req.score());
        // TODO: 双评分差判定 → 仲裁
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/scores/{sessionId}/publish")
    @Operation(summary = "成绩发布")
    @AuditLog(module = "grading", action = "成绩发布")
    public Result<Void> publish(@PathVariable Long sessionId) {
        // TODO: 发布前全量复核;发布触发消息通知
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/scores/{sessionId}/correct")
    @Operation(summary = "成绩更正(需审批留痕)")
    @AuditLog(module = "grading", action = "成绩更正")
    public Result<Void> correct(@PathVariable Long sessionId, @RequestBody CorrectReq req) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record ScoreReq(@NotNull Long taskId, @NotNull Integer score, String comment) {
    }

    public record CorrectReq(String fromValue, String toValue, String reason) {
    }
}
