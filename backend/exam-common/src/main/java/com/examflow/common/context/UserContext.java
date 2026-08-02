package com.examflow.common.context;

import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前用户上下文:从网关透传的 X-User-Id 请求头解析(见 TDD §7.3)。
 * 网关鉴权开启时该头必存在;缺失视为未认证。
 */
public final class UserContext {

    public static final String HEADER_USER_ID = "X-User-Id";

    private UserContext() {
    }

    /** 当前用户 ID;无法解析抛 11001(未登录)。 */
    public static Long requireUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** 当前用户 ID;无上下文返回 null(服务间内部调用场景)。 */
    public static Long currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String header = attrs.getRequest().getHeader(HEADER_USER_ID);
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(header);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
