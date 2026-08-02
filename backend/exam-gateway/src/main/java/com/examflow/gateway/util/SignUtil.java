package com.examflow.gateway.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 用户身份头签名(HmacSHA256)。
 * 网关写入 X-User-Id 时附加 X-User-Sign;服务端 UserContext 用同一密钥校验,
 * 防止绕过网关直连服务时伪造身份(见 TDD §7.3)。
 */
public final class SignUtil {

    private SignUtil() {
    }

    public static String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }
}
