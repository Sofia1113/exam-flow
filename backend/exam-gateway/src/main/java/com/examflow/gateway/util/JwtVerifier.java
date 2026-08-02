package com.examflow.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

/**
 * 网关侧 JWT 校验(HS256)。
 * 与 auth 服务 JwtUtil 共享同一密钥与 claims 定义:
 * sub=userId、token_type=access|refresh、jti —— 修改签名必须两处同步。
 */
public final class JwtVerifier {

    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "access";

    private JwtVerifier() {
    }

    /** 校验签名与有效期,失败抛出 JwtException。 */
    public static Claims parse(String secret, String token) {
        return Jwts.parser()
                .verifyWith(hmacKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey hmacKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
