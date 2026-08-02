package com.examflow.message.controller;

import com.examflow.message.service.MessageService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息内部接口(/internal/** 不经网关):其他服务触发业务通知
 * (报名审核结果/成绩公布等,见 FR-MSG-03)。
 */
@RestController
@RequestMapping("/internal/messages")
@RequiredArgsConstructor
public class InternalMessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public void send(@RequestBody InternalSendReq req) {
        messageService.send(req.templateCode(), req.target(), req.channel(), req.params());
    }

    public record InternalSendReq(String templateCode, String target, String channel,
                                  Map<String, Object> params) {
    }
}
