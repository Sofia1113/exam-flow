package com.examflow.auth.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口(骨架,见 TDD §5.2)。
 * 生产化 TODO:
 * 1. 对接 user-service 校验账号状态与口令(PasswordUtil);
 * 2. 签发 JWT 双令牌(access 30min / refresh 7d,Redis 支持强制下线);
 * 3. 登录频控(5 次/分钟/IP)、图形验证码、连续 5 次失败锁定 30 分钟;
 * 4. 政企内部员工支持 SSO/OAuth2-OIDC/LDAP 对接。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证服务")
public class AuthController {

    @PostMapping("/login")
    @Operation(summary = "账号密码登录")
    @AuditLog(module = "auth", action = "账号密码登录")
    public Result<LoginResp> login(@RequestBody LoginReq req) {
        log.info("登录请求: username={}", req.username());
        // TODO: 校验账号 + 口令,签发双令牌
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/sms/code")
    @Operation(summary = "发送短信验证码")
    public Result<Void> sendSmsCode(@RequestParam @NotBlank String phone) {
        log.info("发送短信验证码: phone={}", phone);
        // TODO: 频控(1 分钟间隔/10 次每日上限)+ 调用 message-service 发送
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/sms/login")
    @Operation(summary = "短信验证码登录")
    public Result<LoginResp> smsLogin(@RequestBody SmsLoginReq req) {
        log.info("短信登录: phone={}", req.phone());
        // TODO: 校验验证码,登录或自动注册
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌")
    public Result<LoginResp> refresh(@RequestBody RefreshReq req) {
        // TODO: 校验 refresh_token(可撤销),签发新 access_token
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password, String captcha) {
    }

    public record SmsLoginReq(@NotBlank String phone, @NotBlank String code) {
    }

    public record RefreshReq(@NotBlank String refreshToken) {
    }

    public record LoginResp(String accessToken, String refreshToken, Long userId, String name) {
    }
}
