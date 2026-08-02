package com.examflow.gateway.filter;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关鉴权过滤器(骨架)。
 * - 白名单路径直接放行(登录、验证码等);
 * - 其余请求校验 Authorization: Bearer <token>;
 * - 生产化 TODO:调用 auth-service 校验 JWT 并解析用户信息写入请求头(X-User-Id 等);
 *   考试相关接口(保存/交卷/心跳)叠加防重放校验(timestamp+nonce+sign,见 TDD §5.3)。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 白名单:登录、验证码、健康检查、接口文档。 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/sms/**",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (WHITE_LIST.stream().anyMatch(p -> PATH_MATCHER.match(p, path))) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith(BEARER_PREFIX) || auth.substring(BEARER_PREFIX.length()).isBlank()) {
            return unauthorized(exchange);
        }
        // TODO: 生产环境在此调用 auth-service 校验令牌并透传用户上下文
        return chain.filter(exchange);
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
