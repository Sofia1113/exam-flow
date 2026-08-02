package com.examflow.user.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import com.examflow.user.dto.CurrentUserPerm;
import com.examflow.user.dto.UserVO;
import com.examflow.user.entity.SysUser;
import com.examflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口(FR-ORG-03):分页(数据权限隔离)、创建、更新、状态、角色分配。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "用户分页(按当前用户数据权限过滤)")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size,
                                           @RequestParam(required = false) Long orgId,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, orgId, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @GetMapping("/permissions/current")
    @Operation(summary = "当前用户权限(菜单/操作控制)")
    public Result<CurrentUserPerm> currentPerm() {
        return Result.ok(userService.currentPerm());
    }

    @PostMapping
    @Operation(summary = "创建用户(密码 bcrypt,可选分配角色)")
    @AuditLog(module = "user", action = "创建用户")
    public Result<Long> create(@RequestBody CreateUserReq req) {
        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPasswordHash(req.password());
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setIdCard(req.idCard());
        user.setOrgId(req.orgId());
        user.setUserType(req.userType() == null ? "external" : req.userType());
        return Result.ok(userService.create(user, req.roleIds()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户基本信息与角色")
    @AuditLog(module = "user", action = "更新用户")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateUserReq req) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setPasswordHash(req.password());
        user.setOrgId(req.orgId());
        userService.update(user, req.roleIds());
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/停用账号")
    @AuditLog(module = "user", action = "变更账号状态")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam @NotBlank String status) {
        if (!List.of("enabled", "disabled", "locked").contains(status)) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }
        userService.changeStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "分配角色(全量替换)")
    @AuditLog(module = "user", action = "用户角色分配")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesReq req) {
        userService.assignRoles(id, req.roleIds());
        return Result.ok();
    }

    public record CreateUserReq(@NotBlank String username, @NotBlank String password,
                                @NotBlank String name, String phone, String idCard,
                                @NotNull Long orgId, String userType, List<Long> roleIds) {
    }

    public record UpdateUserReq(String name, String phone, String password, Long orgId,
                                List<Long> roleIds) {
    }

    public record AssignRolesReq(@NotNull List<Long> roleIds) {
    }
}
