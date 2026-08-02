package com.examflow.exam.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * grading-service 内部调用(交卷后触发客观题判分与成绩核算)。
 * 生产环境:交卷事件走 RocketMQ 异步削峰(见 TDD §3.3),当前同步调用为骨架实现。
 */
@FeignClient(name = "exam-grading", url = "${examflow.grading-service-url:http://127.0.0.1:8088}")
public interface GradingServiceClient {

    @PostMapping("/internal/grading/grade/{sessionId}")
    void grade(@PathVariable("sessionId") Long sessionId);
}
