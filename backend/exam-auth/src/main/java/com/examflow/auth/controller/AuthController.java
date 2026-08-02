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
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口:账号密码登录(JWT 双令牌 + 失败锁定)、短信验证码登录(频控 + 自动注册)、
 * 刷新(轮换)、退出撤销。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证服务")
public class AuthController {

    private static final int MAX_LOGIN_FAIL = 5;
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);
    private static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SMS_INTERVAL = Duration.ofMinutes(1);
    private static final int SMS_DAILY_LIMIT = 10;

    private final UserServiceClient userServiceClient;
    private final TokenService tokenService;
    private final StringRedisTemplate redis;

    @PostMapping("/login")
    @Operation(summary = "账号密码登录:连续 5 次失败锁定 30 分钟")
    @AuditLog(module = "auth", action = "账号密码登录")
    public Result<TokenService.TokenPair> login(@RequestBody LoginReq req) {
        String failKey = "login:fail:" + req.username();
        String failCount = redis.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_LOGIN_FAIL) {
            log.warn("登录被拒(已锁定): username={}", req.username());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "连续登录失败 5 次,账号已锁定 30 分钟");
        }

        UserInfo user = userServiceClient.getByUsername(req.username());
        if (user == null || !PasswordUtil.matches(req.password(), user.passwordHash())) {
            Long count = redis.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                redis.expire(failKey, LOCK_TTL);
            }
            log.warn("登录失败: username={}, 失败次数={}", req.username(), count);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        redis.delete(failKey);
        if (!"enabled".equals(user.status())) {
            log.warn("登录被拒: username={}, 账号状态={}", req.username(), user.status());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        return Result.ok(tokenService.issue(user.userId(), user.username()));
    }

    @GetMapping("/sms/code")
    @Operation(summary = "发送短信验证码(1 分钟 1 条,每日 10 条)")
    public Result<Void> sendSmsCode(@RequestParam @NotBlank String phone) {
        // 频控:1 分钟 1 条
        if (Boolean.TRUE.equals(redis.hasKey("sms:last:" + phone))) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "发送过于频繁,请 1 分钟后再试");
        }
        // 频控:每日 10 条
        String dailyKey = "sms:daily:" + phone + ":" + LocalDate.now();
        Long daily = redis.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1) {
            redis.expire(dailyKey, Duration.ofHours(24));
        }
        if (daily != null && daily > SMS_DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "今日验证码发送次数已达上限");
        }

        String code = String.valueOf(100000 + ThreadLocalRandom.current().nextInt(900000));
        redis.opsForValue().set("sms:code:" + phone, code, SMS_CODE_TTL);
        redis.opsForValue().set("sms:last:" + phone, "1", SMS_INTERVAL);
        // TODO: 生产接入短信通道(message-service);当前仅日志模拟
        log.info("[短信模拟] phone={}, 验证码={}, 5 分钟内有效", phone, code);
        return Result.ok();
    }

    @PostMapping("/sms/login")
    @Operation(summary = "短信验证码登录(未注册手机号自动注册)")
    @AuditLog(module = "auth", action = "短信验证码登录")
    public Result<TokenService.TokenPair> smsLogin(@RequestBody SmsLoginReq req) {
        String code = redis.opsForValue().get("sms:code:" + req.phone());
        if (code == null || !code.equals(req.code())) {
            throw new BusinessException(ErrorCode.SMS_CODE_ERROR);
        }
        redis.delete("sms:code:" + req.phone());

        UserInfo user = userServiceClient.getByPhone(req.phone());
        if (user == null) {
            // 未注册:自动注册为 external 考生(FR-AUTH-01)
            userServiceClient.register(new com.examflow.auth.dto.RegisterReq(req.phone()));
            user = userServiceClient.getByUsername("u" + req.phone());
        }
        if (user == null || !"enabled".equals(user.status())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        log.info("短信登录成功: phone={}, userId={}", req.phone(), user.userId());
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

    public record LoginReq(@NotBlank String username, @NotBlank String password, String captcha) {
    }

    public record SmsLoginReq(@NotBlank String phone, @NotBlank String code) {
    }

    public record RefreshReq(@NotBlank String refreshToken) {
    }

    public record LogoutReq(String refreshToken) {
    }
}
