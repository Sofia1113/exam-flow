package com.examflow.message.controller;

import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息接口(骨架,见 PRD FR-MSG)。
 * 生产化 TODO:
 * 1. 消费通知事件,按模板渲染并分渠道发送(短信 ≥2 家供应商容灾,失败自动切换);
 * 2. 发送失败重试 3 次,站内信兜底必达;
 * 3. 批量触达 ≤10 万条/小时,队列削峰。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "消息服务")
public class MessageController {

    @GetMapping("/notifications")
    @Operation(summary = "站内信列表(按当前用户)")
    public Result<PageResult<Object>> notifications(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/send")
    @Operation(summary = "发送通知(按模板)")
    public Result<Void> send(@RequestBody SendReq req) {
        log.info("发送通知: template={}, target={}", req.templateCode(), req.target());
        // TODO: 模板渲染 + 渠道选择 + 失败重试
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/records")
    @Operation(summary = "发送记录与回执")
    public Result<PageResult<Object>> records(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "20") long size) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record SendReq(@NotBlank String templateCode, @NotBlank String target,
                          String channel, Object params) {
    }
}
