package com.examflow.user.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import com.examflow.user.dto.RoleVO;
import com.examflow.user.entity.SysRole;
import com.examflow.user.service.RoleService;
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
 * 角色与权限接口(FR-ORG-02):角色 CRUD、权限码分配、数据权限范围。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "角色权限管理")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "角色分页")
    public Result<PageResult<RoleVO>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size) {
        return Result.ok(roleService.page(page, size));
    }

    @GetMapping("/all")
    @Operation(summary = "全部角色(下拉选择用)")
    public Result<List<RoleVO>> listAll() {
        return Result.ok(roleService.listAll());
    }

    @PostMapping
    @Operation(summary = "创建角色")
    @AuditLog(module = "user", action = "创建角色")
    public Result<Long> create(@RequestBody RoleReq req) {
        SysRole role = new SysRole();
        role.setCode(req.code());
        role.setName(req.name());
        role.setRemark(req.remark());
        return Result.ok(roleService.create(role));
    }

    @PutMapping("/{id}/perms")
    @Operation(summary = "分配权限码(全量替换)")
    @AuditLog(module = "user", action = "角色权限分配")
    public Result<Void> assignPerms(@PathVariable Long id, @RequestBody PermReq req) {
        roleService.assignPerms(id, req.permCodes());
        return Result.ok();
    }

    @PutMapping("/{id}/data-scope")
    @Operation(summary = "设置数据权限范围(all/current/children)")
    @AuditLog(module = "user", action = "角色数据权限设置")
    public Result<Void> setDataScope(@PathVariable Long id, @RequestBody DataScopeReq req) {
        roleService.setDataScope(id, req.scopeType(), req.orgIds());
        return Result.ok();
    }

    public record RoleReq(@NotBlank String code, @NotBlank String name, String remark) {
    }

    public record PermReq(@NotNull List<String> permCodes) {
    }

    public record DataScopeReq(@NotBlank String scopeType, String orgIds) {
    }
}
