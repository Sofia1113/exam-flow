package com.examflow.grading.controller;

import com.examflow.grading.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阅卷内部接口(/internal/** 不经网关):交卷事件触发客观题判分。
 * 生产环境:消费 RocketMQ"交卷完成"事件(见 TDD §3.3),当前同步调用为骨架实现。
 */
@RestController
@RequestMapping("/internal/grading")
@RequiredArgsConstructor
public class InternalGradingController {

    private final GradingService gradingService;

    @PostMapping("/grade/{sessionId}")
    public void grade(@PathVariable Long sessionId) {
        gradingService.grade(sessionId);
    }
}
