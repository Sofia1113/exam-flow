package com.examflow.message.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examflow.common.audit.AuditLog;
import com.examflow.common.context.UserContext;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import com.examflow.message.entity.NotifyRecord;
import com.examflow.message.mapper.NotifyRecordMapper;
import com.examflow.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息接口(FR-MSG):发送(管理端触发)、站内信、记录查询。
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "消息服务")
public class MessageController {

    private final MessageService messageService;
    private final NotifyRecordMapper recordMapper;

    @GetMapping("/notifications")
    @Operation(summary = "我的站内信")
    public Result<List<NotifyRecord>> notifications() {
        return Result.ok(messageService.myNotifications(String.valueOf(UserContext.requireUserId())));
    }

    @PostMapping("/send")
    @Operation(summary = "发送通知(按模板)")
    @AuditLog(module = "message", action = "发送通知")
    public Result<Void> send(@RequestBody SendReq req) {
        messageService.send(req.templateCode(), req.target(), req.channel(), req.params());
        return Result.ok();
    }

    @GetMapping("/records")
    @Operation(summary = "发送记录")
    public Result<PageResult<NotifyRecord>> records(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size) {
        Page<NotifyRecord> p = recordMapper.selectPage(new Page<>(page, size),
                Wrappers.lambdaQuery(NotifyRecord.class).orderByDesc(NotifyRecord::getId));
        return Result.ok(PageResult.of(p));
    }

    public record SendReq(@NotBlank String templateCode, @NotBlank String target,
                          String channel, Map<String, Object> params) {
    }
}
