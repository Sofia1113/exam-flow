package com.examflow.user.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户与组织接口(骨架,见 PRD FR-ORG)。
 * 生产化 TODO:
 * 1. 组织树(多级)+ 员工离职自动停用;
 * 2. RBAC:角色/菜单权限/数据权限(组织范围 SQL 层拦截);
 * 3. 账号信息对身份证/手机号加密存储与脱敏展示。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户组织服务")
public class UserController {

    @GetMapping
    @Operation(summary = "分页查询用户(按组织范围)")
    public Result<PageResult<Object>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size) {
        // TODO: 数据权限拦截,仅返回授权组织范围
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public Result<Object> detail(@PathVariable @NotNull Long id) {
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @AuditLog(module = "user", action = "创建用户")
    public Result<Void> create(@RequestBody CreateUserReq req) {
        log.info("创建用户: username={}", req.username());
        // TODO: 用户名唯一校验、密码 bcrypt 加密、组织归属校验
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/停用账号")
    @AuditLog(module = "user", action = "变更账号状态")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam @NotBlank String status) {
        // TODO: 停用账号时同步撤销令牌
        return Result.fail(ErrorCode.UNIMPLEMENTED);
    }

    public record CreateUserReq(@NotBlank String username, @NotBlank String password,
                                @NotBlank String name, String phone, String idCard,
                                @NotNull Long orgId, String userType) {
    }
}
