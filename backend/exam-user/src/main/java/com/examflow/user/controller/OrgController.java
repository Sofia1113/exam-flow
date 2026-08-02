package com.examflow.user.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.Result;
import com.examflow.user.dto.OrgNode;
import com.examflow.user.entity.SysOrg;
import com.examflow.user.service.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组织管理接口(FR-ORG-01):组织树/CRUD。
 */
@RestController
@RequestMapping("/api/v1/orgs")
@RequiredArgsConstructor
@Tag(name = "组织管理")
public class OrgController {

    private final OrgService orgService;

    @GetMapping("/tree")
    @Operation(summary = "组织树(全量)")
    public Result<List<OrgNode>> tree() {
        return Result.ok(orgService.tree());
    }

    @PostMapping
    @Operation(summary = "创建组织")
    @AuditLog(module = "user", action = "创建组织")
    public Result<Long> create(@RequestBody OrgReq req) {
        SysOrg org = new SysOrg();
        org.setParentId(req.parentId());
        org.setName(req.name());
        org.setOrgType(req.orgType() == null ? "dept" : req.orgType());
        return Result.ok(orgService.create(org));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新组织")
    @AuditLog(module = "user", action = "更新组织")
    public Result<Void> update(@PathVariable Long id, @RequestBody OrgReq req) {
        SysOrg org = new SysOrg();
        org.setId(id);
        org.setName(req.name());
        org.setOrgType(req.orgType());
        orgService.update(org);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织(有子组织或用户时拒绝)")
    @AuditLog(module = "user", action = "删除组织")
    public Result<Void> delete(@PathVariable Long id) {
        orgService.delete(id);
        return Result.ok();
    }

    public record OrgReq(Long parentId, @NotBlank String name, String orgType) {
    }
}
