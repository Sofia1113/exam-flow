package com.examflow.auth.controller;

import com.examflow.auth.client.UserServiceClient;
import com.examflow.auth.dto.UserInfo;
import com.examflow.auth.service.TokenService;
import com.examflow.auth.util.JwtUtil;
import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.Result;
import com.examflow.common.util.PasswordUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口:账号密码登录(JWT 双令牌)、刷新、退出撤销。
 * 安全要求:登录频控(5 次/分钟/IP)、连续 5 次失败锁定 30 分钟(FR-AUTH-02)待 M1-005 落地。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证服务")
public class AuthController {

    private final UserServiceClient userServiceClient;
    private final TokenService tokenService;

    @PostMapping("/login")
    @Operation(summary = "账号密码登录,签发 access+refresh 双令牌")
    @AuditLog(module = "auth", action = "账号密码登录")
    public Result<TokenService.TokenPair> login(@RequestBody LoginReq req) {
        UserInfo user = userServiceClient.getByUsername(req.username());
        if (user == null || !PasswordUtil.matches(req.password(), user.passwordHash())) {
            log.warn("登录失败: username={}, 账号或密码错误", req.username());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!"enabled".equals(user.status())) {
            log.warn("登录被拒: username={}, 账号状态={}", req.username(), user.status());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        return Result.ok(tokenService.issue(user.userId(), user.username()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "凭 refresh 令牌换发新 access + refresh(轮换)")
    @AuditLog(module = "auth", action = "刷新令牌")
    public Result<TokenService.TokenPair> refresh(@RequestBody RefreshReq req) {
        Claims claims = tokenService.verify(req.refreshToken(), JwtUtil.TOKEN_TYPE_REFRESH);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        // 安全实践:refresh 令牌轮换 —— 每次使用即撤销旧令牌并签发新对,
        // 被盗令牌一旦被使用即失效,且可据黑名单检测重放(TDD §7.1)
        tokenService.revoke(req.refreshToken());
        return Result.ok(tokenService.issue(userId, username));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录:撤销 access 与 refresh 令牌")
    @AuditLog(module = "auth", action = "退出登录")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization,
                               @RequestBody(required = false) LogoutReq req) {
        // 撤销当前 access
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenService.revoke(authorization.substring(7));
        }
        // 撤销 refresh(客户端传回,可选)
        if (req != null && req.refreshToken() != null && !req.refreshToken().isBlank()) {
            tokenService.revoke(req.refreshToken());
        }
        return Result.ok();
    }

    @GetMapping("/sms/code")
    @Operation(summary = "发送短信验证码(M1-004 实现)")
    public Result<Void> sendSmsCode(@RequestParam @NotBlank String phone) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/sms/login")
    @Operation(summary = "短信验证码登录(M1-004 实现)")
    public Result<TokenService.TokenPair> smsLogin(@RequestBody SmsLoginReq req) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password, String captcha) {
    }

    public record SmsLoginReq(@NotBlank String phone, @NotBlank String code) {
    }

    public record RefreshReq(@NotBlank String refreshToken) {
    }

    public record LogoutReq(String refreshToken) {
    }
}
