package com.examflow.auth.service;

import com.examflow.auth.config.JwtProperties;
import com.examflow.auth.util.JwtUtil;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 令牌服务:签发双令牌、校验(签名/类型/黑名单)、撤销(Redis 黑名单)。
 *
 * <p>可撤销机制(TDD §7.1):退出/强制下线时将 jti 写入 Redis 黑名单,
 * 键 = blacklistPrefix:token_type:jti,TTL = 令牌剩余有效期;网关校验时查询黑名单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redis;

    /** 签发 access + refresh 双令牌。 */
    public TokenPair issue(Long userId, String username) {
        String access = JwtUtil.sign(jwtProperties.getSecret(), userId, username,
                JwtUtil.TOKEN_TYPE_ACCESS, jwtProperties.getAccessTtl());
        String refresh = JwtUtil.sign(jwtProperties.getSecret(), userId, username,
                JwtUtil.TOKEN_TYPE_REFRESH, jwtProperties.getRefreshTtl());
        log.info("签发令牌: userId={}, username={}", userId, username);
        return new TokenPair(access, refresh, jwtProperties.getAccessTtl().toSeconds(), userId, username);
    }

    /** 校验令牌:签名、token_type、黑名单;任一不满足抛 TOKEN_INVALID。 */
    public Claims verify(String token, String expectedType) {
        Claims claims = JwtUtil.parse(jwtProperties.getSecret(), token);
        if (!expectedType.equals(claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "令牌类型不符");
        }
        if (isBlacklisted(claims.getId())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "令牌已撤销");
        }
        return claims;
    }

    /** 撤销令牌(加入黑名单,TTL = 剩余有效期)。 */
    public void revoke(String token) {
        Claims claims;
        try {
            claims = JwtUtil.parse(jwtProperties.getSecret(), token);
        } catch (Exception e) {
            // 已失效令牌无需撤销
            return;
        }
        String key = blacklistKey(claims);
        Duration ttl = JwtUtil.remainingTtl(claims);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.opsForValue().set(key, "1", ttl);
        log.info("令牌已撤销: jti={}, ttl={}s", claims.getId(), ttl.toSeconds());
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(blacklistPrefix(jti)));
    }

    private String blacklistKey(Claims claims) {
        return blacklistPrefix(claims.getId());
    }

    /** 网关校验黑名单时使用同一 key 规则:blacklistPrefix:token_type:jti */
    private String blacklistPrefix(String jti) {
        return jwtProperties.getBlacklistPrefix() + ":access:" + jti;
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn,
                            Long userId, String username) {
    }
}
