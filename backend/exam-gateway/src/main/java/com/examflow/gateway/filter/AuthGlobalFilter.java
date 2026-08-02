package com.examflow.gateway.filter;

import com.examflow.gateway.util.JwtVerifier;
import io.jsonwebtoken.Claims;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关鉴权过滤器:真实 JWT 校验 + 黑名单 + 用户上下文透传。
 *
 * <ul>
 *   <li>白名单路径(登录/验证码/刷新/健康检查/文档)直接放行;</li>
 *   <li>其余请求:校验 JWT 签名/有效期、token_type=access、Redis 黑名单(jti);</li>
 *   <li>校验通过后写入 X-User-Id 请求头,供下游服务做资源归属校验(IDOR 防护);</li>
 *   <li>{@code examflow.security.auth-enabled=false} 仅限本地联调,严禁生产开启。</li>
 * </ul>
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 白名单:登录、验证码、刷新令牌、健康检查、接口文档。 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/sms/**",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**"
    );

    @Value("${examflow.security.auth-enabled:true}")
    private boolean authEnabled;

    /** 与 auth 服务共享密钥(HS256),生产经 JWT_SECRET 注入 */
    @Value("${examflow.jwt.secret:exam-flow-dev-jwt-secret-key-0123456789abcdef}")
    private String jwtSecret;

    /** 黑名单键前缀,与 auth 服务 JwtProperties.blacklistPrefix 一致 */
    @Value("${examflow.jwt.blacklist-prefix:examflow:jwt:blacklist}")
    private String blacklistPrefix;

    private final ReactiveStringRedisTemplate redis;

    public AuthGlobalFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (WHITE_LIST.stream().anyMatch(p -> PATH_MATCHER.match(p, path))) {
            return chain.filter(exchange);
        }
        if (!authEnabled) {
            log.warn("网关鉴权已关闭(auth-enabled=false),仅限本地联调,严禁用于生产");
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith(BEARER_PREFIX) || auth.substring(BEARER_PREFIX.length()).isBlank()) {
            return unauthorized(exchange);
        }
        String token = auth.substring(BEARER_PREFIX.length()).trim();

        final Claims claims;
        try {
            claims = JwtVerifier.parse(jwtSecret, token);
            if (!JwtVerifier.TOKEN_TYPE_ACCESS.equals(claims.get(JwtVerifier.CLAIM_TOKEN_TYPE, String.class))) {
                log.debug("令牌类型不符,拒绝: type={}", claims.get(JwtVerifier.CLAIM_TOKEN_TYPE));
                return unauthorized(exchange);
            }
        } catch (Exception e) {
            log.debug("令牌校验失败: {}", e.getMessage());
            return unauthorized(exchange);
        }

        // 黑名单校验(退出/强制下线后立即失效)
        String blacklistKey = blacklistPrefix + ":access:" + claims.getId();
        return redis.hasKey(blacklistKey)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        log.debug("令牌已撤销,拒绝: jti={}", claims.getId());
                        return unauthorized(exchange);
                    }
                    // 透传用户上下文:附带 HMAC 签名防伪造 —— 服务端校验
                    // X-User-Sign 通过后才信任 X-User-Id(见 UserContext)
                    String sign = com.examflow.gateway.util.SignUtil.hmac(jwtSecret, claims.getSubject());
                    ServerWebExchange mutated = exchange.mutate()
                            .request(r -> r.header("X-User-Id", claims.getSubject())
                                    .header("X-User-Sign", sign))
                            .build();
                    return chain.filter(mutated);
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
