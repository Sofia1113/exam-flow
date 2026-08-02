package com.examflow.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 审计切面(骨架):记录操作人、动作、耗时与结果。
 * 生产化 TODO:
 * 1. 操作人从安全上下文(登录令牌)解析,替代占位 "anonymous";
 * 2. 异步发布审计事件(RocketMQ)到 sys-service 的独立只追加审计存储;
 * 3. 敏感对象(试卷/答卷/成绩)记录 before/after JSON 对比。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            success = false;
            errorMsg = t.getMessage();
            throw t;
        } finally {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("module", auditLog.module());
            record.put("action", auditLog.action());
            record.put("operator", "anonymous"); // TODO: 从安全上下文解析操作人
            record.put("method", pjp.getSignature().toShortString());
            record.put("success", success);
            record.put("error", errorMsg);
            record.put("costMs", System.currentTimeMillis() - start);
            record.put("time", LocalDateTime.now().toString());
            try {
                log.info("AUDIT {}", objectMapper.writeValueAsString(record));
            } catch (Exception e) {
                log.error("审计日志序列化失败", e);
            }
        }
    }
}
