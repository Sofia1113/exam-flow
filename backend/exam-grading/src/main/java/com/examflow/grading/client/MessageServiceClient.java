package com.examflow.grading.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * message-service 内部调用(成绩公布通知)。
 */
@FeignClient(name = "exam-message", url = "${examflow.message-service-url:http://127.0.0.1:8090}")
public interface MessageServiceClient {

    @PostMapping("/internal/messages/send")
    void send(@RequestBody SendReq req);

    record SendReq(String templateCode, String target, String channel, Map<String, Object> params) {
    }
}
