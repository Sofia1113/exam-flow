package com.examflow.proctor.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监考接口(骨架,见 PRD FR-PROCTOR)。
 * 生产化 TODO:
 * 1. 风险引擎:切屏≥3 次标记疑似作弊,≥6 次可配置强制交卷(阈值走 sys-service 参数);
 * 2. 监考台实时视图(Redis 在线状态 + 风险计数);
 * 3. 处置(警告/强制交卷/作废)调用 exam-service,全程留痕;
 * 4. 作弊申诉复核闭环 ≤ 3 个工作日。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/proctor")
@RequiredArgsConstructor
@Tag(name = "监考服务")
public class ProctorController {

    @GetMapping("/slots/{slotId}/sessions")
    @Operation(summary = "监考台:场次内考生实时状态(风险置顶)")
    public Result<PageResult<Object>> liveView(@PathVariable @NotNull Long slotId) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/sessions/{sessionId}/behaviors")
    @Operation(summary = "考生行为明细(监察取证)")
    public Result<Object> behaviors(@PathVariable Long sessionId,
                                    @RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "50") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/sessions/{sessionId}/warn")
    @Operation(summary = "发送警告弹窗")
    @AuditLog(module = "proctor", action = "发送考试警告")
    public Result<Void> warn(@PathVariable Long sessionId, @RequestParam @NotBlank String reason) {
        log.info("警告考生: session={}, reason={}", sessionId, reason);
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/sessions/{sessionId}/force-submit")
    @Operation(summary = "强制交卷")
    @AuditLog(module = "proctor", action = "强制交卷")
    public Result<Void> forceSubmit(@PathVariable Long sessionId, @RequestParam @NotBlank String reason) {
        // TODO: 调用 exam-service 强制交卷,记录处置原因
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/sessions/{sessionId}/void")
    @Operation(summary = "作废答卷(需二次验证)")
    @AuditLog(module = "proctor", action = "作废答卷")
    public Result<Void> voidAnswer(@PathVariable Long sessionId, @RequestParam @NotBlank String reason,
                                   @RequestParam @NotBlank String verifyCode) {
        // TODO: 敏感操作二次验证(短信/动态口令)
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }
}
