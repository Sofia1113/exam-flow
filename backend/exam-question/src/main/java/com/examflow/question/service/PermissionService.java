package com.examflow.question.service;

import com.examflow.common.context.UserContext;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.question.client.UserServiceClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 方法级授权(骨架实现):从 user-service 拉取当前用户权限码,校验包含目标权限。
 * 权限码:"*"(超级管理员)或具体权限(question:view / question:edit / question:audit)。
 * 后续可演进为注解 + AOP 统一实现(X-001 范围内)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserServiceClient userServiceClient;

    /** 校验当前用户具备权限,否则抛 12001。 */
    public void requirePerm(String perm) {
        Long userId = UserContext.requireUserId();
        List<String> perms;
        try {
            perms = userServiceClient.getPerms(userId);
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限服务暂不可用");
        }
        if (!perms.contains("*") && !perms.contains(perm)) {
            log.warn("越权访问被拒: userId={}, 需要权限={}, 实际={}", userId, perm, perms);
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限执行该操作");
        }
    }
}
