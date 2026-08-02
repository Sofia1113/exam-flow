package com.examflow.auth.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置(examflow.jwt.*)。
 * 密钥与网关共享(HS256 对称签名),生产必须通过环境变量 JWT_SECRET 注入且 ≥ 32 字节。
 */
@Data
@ConfigurationProperties(prefix = "examflow.jwt")
public class JwtProperties {

    /** 签名密钥(≥ 32 字节),生产经 JWT_SECRET 注入 */
    private String secret;

    /** access 令牌有效期,默认 30 分钟 */
    private Duration accessTtl = Duration.ofMinutes(30);

    /** refresh 令牌有效期,默认 7 天 */
    private Duration refreshTtl = Duration.ofDays(7);

    /** Redis 黑名单键前缀 */
    private String blacklistPrefix = "examflow:jwt:blacklist";
}
