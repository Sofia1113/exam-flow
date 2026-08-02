package com.examflow.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * JWT 双令牌工具(HS256)。
 * Claims:sub=userId、username、token_type(access|refresh)、jti(唯一,可撤销用)。
 * 网关侧使用同名密钥校验(JwtVerifier),签名与 claims 定义必须保持一致。
 */
public final class JwtUtil {

    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private JwtUtil() {
    }

    public static String sign(String secret, Long userId, String username, String tokenType, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(hmacKey(secret))
                .compact();
    }

    /** 解析并校验签名/有效期,失败抛出 JwtException。 */
    public static Claims parse(String secret, String token) {
        return Jwts.parser()
                .verifyWith(hmacKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 令牌剩余有效时长(用于黑名单 TTL)。 */
    public static Duration remainingTtl(Claims claims) {
        return Duration.between(Instant.now(), claims.getExpiration().toInstant());
    }

    private static SecretKey hmacKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
