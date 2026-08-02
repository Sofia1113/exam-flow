package com.examflow.exam.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.Result;
import com.examflow.exam.service.ExamSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线考试接口(核心链路,见 TDD §5.2)。
 * 交卷/保存/心跳为最高优先级接口,网关叠加防重放签名校验。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/exam")
@RequiredArgsConstructor
@Tag(name = "在线考试服务")
public class ExamController {

    private final ExamSessionService examSessionService;

    @PostMapping("/sessions")
    @Operation(summary = "进入考试(校验 + 抽卷 + 创建会话)")
    @AuditLog(module = "exam", action = "进入考试")
    public Result<Object> enter(@RequestBody EnterReq req) {
        return Result.ok(examSessionService.enter(req.registrationId(), req.clientIp(), req.deviceFp()));
    }

    @PostMapping("/sessions/{id}/resume")
    @Operation(summary = "断线恢复(返回已存答案与剩余时间)")
    public Result<Object> resume(@PathVariable Long id, @RequestBody ResumeReq req) {
        return Result.ok(examSessionService.resume(id, req.registrationId()));
    }

    @PostMapping("/sessions/{id}/answers")
    @Operation(summary = "保存作答增量(seq 对齐)")
    public Result<Long> saveAnswers(@PathVariable Long id, @RequestBody SaveReq req) {
        long lastSeq = examSessionService.saveAnswers(id, req.registrationId(), req.fromSeq(), req.answers());
        return Result.ok(lastSeq);
    }

    @PostMapping("/sessions/{id}/heartbeat")
    @Operation(summary = "心跳(在线状态)")
    public Result<Void> heartbeat(@PathVariable Long id, @RequestBody ResumeReq req) {
        examSessionService.heartbeat(id, req.registrationId());
        return Result.ok();
    }

    @PostMapping("/sessions/{id}/submit")
    @Operation(summary = "交卷(幂等,先落库后确认)")
    @AuditLog(module = "exam", action = "交卷")
    public Result<Void> submit(@PathVariable Long id, @RequestBody SubmitReq req) {
        examSessionService.submit(id, req.registrationId(), req.answers(), req.sign());
        return Result.ok();
    }

    @PostMapping("/sessions/{id}/behaviors")
    @Operation(summary = "行为事件批量上报(切屏/离屏等)")
    public Result<Void> reportBehaviors(@PathVariable Long id, @RequestBody BehaviorReq req) {
        // TODO: 异步写入 exam_behavior_log(分片),供风险引擎消费
        log.info("行为上报: session={}, count={}", id, req.events() == null ? 0 : req.events().size());
        return Result.ok();
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "查询会话状态与剩余时间")
    public Result<Object> detail(@PathVariable Long id) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record EnterReq(@NotNull Long registrationId, String clientIp, String deviceFp, String faceToken) {
    }

    public record ResumeReq(@NotNull Long registrationId) {
    }

    public record SaveReq(@NotNull Long registrationId, long fromSeq,
                          @NotNull List<Map<String, Object>> answers) {
    }

    public record SubmitReq(@NotNull Long registrationId, @NotNull List<Map<String, Object>> answers,
                            @NotBlank String sign) {
    }

    public record BehaviorReq(@NotNull Long registrationId, List<Map<String, Object>> events) {
    }
}
