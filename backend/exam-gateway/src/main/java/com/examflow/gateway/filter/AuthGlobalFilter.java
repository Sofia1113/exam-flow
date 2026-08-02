package com.examflow.gateway.filter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * 网关鉴权过滤器。
 *
 * <p>安全基线(fail-closed):JWT 签名校验实现完成之前,非白名单请求一律拒绝,
 * 禁止"仅检查令牌格式后放行" —— 那等同于认证绕过。
 *
 * <ul>
 *   <li>白名单路径(登录/验证码/健康检查/接口文档)直接放行;</li>
 *   <li>其余请求校验 Authorization: Bearer &lt;token&gt;,并在校验通过后
 *       透传用户上下文(X-User-Id 等请求头)供下游资源归属校验;</li>
 *   <li>{@code examflow.security.auth-enabled=false} 仅用于本地联调显式关闭,
 *       严禁在生产开启。</li>
 * </ul>
 */
@Slf4j
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

    /** 鉴权开关:默认开启(fail-closed);仅本地联调可显式关闭。 */
    @Value("${examflow.security.auth-enabled:true}")
    private boolean authEnabled;

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
        // TODO: 生产化调用 auth-service 校验 JWT(签名/有效期/黑名单),解析用户信息
        // 写入请求头 X-User-Id / X-User-Roles 供下游服务做资源归属与权限校验。
        // 在真实校验实现完成前,一律拒绝(fail-closed),不放过任意格式令牌。
        return unauthorized(exchange);
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
