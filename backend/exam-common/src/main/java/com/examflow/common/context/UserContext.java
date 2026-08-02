package com.examflow.common.context;

import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前用户上下文:解析并验证网关透传的身份头(见 TDD §7.3)。
 *
 * <p>安全基线(防伪造):客户端无法直接访问服务端口(仅经网关转发),但仍不能信任
 * 明文 X-User-Id —— 网关写入该头时附加 HMAC-SHA256 签名(X-User-Sign),
 * 本类校验签名通过后才采用用户 ID,防止服务被绕过网关直连时伪造身份。
 *
 * <p>签名密钥与网关共享:环境变量 JWT_SECRET(≥ 32 字节),未配置时回退开发默认值
 * (仅本地联调,严禁生产)。
 */
public final class UserContext {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_SIGN = "X-User-Sign";

    /** 与网关 yml 默认值保持一致;生产必须注入 JWT_SECRET。 */
    private static final String DEFAULT_SECRET = "exam-flow-dev-jwt-secret-key-0123456789abcdef";

    private static final String SECRET = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET") : DEFAULT_SECRET;

    private UserContext() {
    }

    /** 当前用户 ID;签名校验失败或缺失抛 11001(未登录)。 */
    public static Long requireUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** 当前用户 ID;无上下文或签名无效返回 null(服务间内部调用场景)。 */
    public static Long currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String userId = attrs.getRequest().getHeader(HEADER_USER_ID);
        String sign = attrs.getRequest().getHeader(HEADER_USER_SIGN);
        if (userId == null || userId.isBlank() || sign == null || sign.isBlank()) {
            return null;
        }
        if (!verifySign(userId, sign)) {
            return null;
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 校验签名:sign = HmacSHA256(secret, userId) 的 Base64。 */
    public static boolean verifySign(String userId, String sign) {
        String expect = hmac(userId);
        return expect != null && constantTimeEquals(expect, sign);
    }

    /** 生成签名(网关侧写入头时调用;实现与校验侧保持一致)。 */
    public static String sign(String userId) {
        return hmac(userId);
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
