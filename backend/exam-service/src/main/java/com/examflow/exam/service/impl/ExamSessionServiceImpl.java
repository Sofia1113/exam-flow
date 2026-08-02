package com.examflow.exam.service.impl;

import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.exam.entity.ExamSession;
import com.examflow.exam.service.ExamSessionService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 考试会话服务实现(骨架占位,业务逻辑待实现)。
 * TODO:
 * 1. 进入:校验时间窗(迟到 30 分钟禁入)、状态机、seed 抽卷、创建会话(分布式锁防并发双击);
 * 2. 保存:seq 对齐(13005 类错误码)、RocketMQ 事务消息 + 批量写 worker;
 * 3. 交卷:setnx 幂等锁、完整性校验、本地事务落库、发"交卷完成"事件(GradingService 消费判分);
 * 4. 心跳:Redis 更新在线状态(TTL 60s)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    @Override
    public ExamSession enter(Long registrationId, String clientIp, String deviceFp) {
        throw new BusinessException(ErrorCode.UNIMPLEMENTED);
    }

    @Override
    public ExamSession resume(Long sessionId, Long registrationId) {
        throw new BusinessException(ErrorCode.UNIMPLEMENTED);
    }

    @Override
    public long saveAnswers(Long sessionId, Long registrationId, long fromSeq,
                            List<Map<String, Object>> answers) {
        throw new BusinessException(ErrorCode.UNIMPLEMENTED);
    }

    @Override
    public void heartbeat(Long sessionId, Long registrationId) {
        throw new BusinessException(ErrorCode.UNIMPLEMENTED);
    }

    @Override
    public void submit(Long sessionId, Long registrationId, List<Map<String, Object>> answers, String sign) {
        throw new BusinessException(ErrorCode.UNIMPLEMENTED);
    }
}
