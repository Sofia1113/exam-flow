package com.examflow.exam.service;

import com.examflow.exam.dto.ExamDTO;
import java.util.List;
import java.util.Map;

/**
 * 考试会话服务。
 *
 * <p>生产化要点(TDD §3.2/§3.3/§6):
 * <ul>
 *   <li>保存:本地事务 + RocketMQ 事务消息 → 批量消费 worker 合并 upsert,seq 对齐防乱序;</li>
 *   <li>交卷:Redis setnx 锁 + 唯一索引保证幂等,先落库后 ack,客户端指数退避重试;</li>
 *   <li>倒计时:以服务端 deadlineAt 为准,客户端不可伪造。</li>
 * </ul>
 */
public interface ExamSessionService {

    /** 进入考试:校验场次时间窗/已交卷/进入次数,抽卷并创建会话。 */
    ExamDTO.EnterResp enter(Long registrationId, String clientIp, String deviceFp);

    /** 断线恢复:返回已落库明细与 lastSeq,剩余时间以服务器计算。 */
    ExamDTO.ResumeResp resume(Long sessionId, Long registrationId);

    /** 保存作答增量:返回服务端最新 lastSeq(客户端从断点重发)。 */
    long saveAnswers(Long sessionId, Long registrationId, long fromSeq, List<Map<String, Object>> answers);

    /** 心跳:更新在线状态。 */
    void heartbeat(Long sessionId, Long registrationId);

    /** 交卷:幂等提交,落库后触发判分。 */
    void submit(Long sessionId, Long registrationId, List<Map<String, Object>> answers, String sign);
}
