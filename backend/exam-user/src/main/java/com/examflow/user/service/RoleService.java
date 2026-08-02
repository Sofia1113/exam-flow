package com.examflow.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.user.dto.RoleVO;
import com.examflow.user.entity.SysDataScope;
import com.examflow.user.entity.SysRole;
import com.examflow.user.entity.SysRolePerm;
import com.examflow.user.mapper.DataScopeMapper;
import com.examflow.user.mapper.RoleMapper;
import com.examflow.user.mapper.RolePermMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色服务:CRUD、权限码分配、数据权限范围。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RolePermMapper rolePermMapper;
    private final DataScopeMapper dataScopeMapper;

    public PageResult<RoleVO> page(long page, long size) {
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, size),
                Wrappers.lambdaQuery(SysRole.class).orderByAsc(SysRole::getId));
        return PageResult.of(p.convert(this::toVO));
    }

    public List<RoleVO> listAll() {
        return roleMapper.selectList(Wrappers.lambdaQuery(SysRole.class)
                        .orderByAsc(SysRole::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public Long create(SysRole role) {
        Long exists = roleMapper.selectCount(Wrappers.lambdaQuery(SysRole.class)
                .eq(SysRole::getCode, role.getCode()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "角色编码已存在");
        }
        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional
    public void update(SysRole role) {
        SysRole exist = roleMapper.selectById(role.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (role.getName() != null) {
            exist.setName(role.getName());
        }
        if (role.getRemark() != null) {
            exist.setRemark(role.getRemark());
        }
        roleMapper.updateById(exist);
    }

    /** 分配权限码(全量替换)。 */
    @Transactional
    public void assignPerms(Long roleId, List<String> permCodes) {
        checkRole(roleId);
        rolePermMapper.delete(Wrappers.lambdaQuery(SysRolePerm.class)
                .eq(SysRolePerm::getRoleId, roleId));
        for (String code : permCodes) {
            SysRolePerm perm = new SysRolePerm();
            perm.setRoleId(roleId);
            perm.setPermCode(code);
            rolePermMapper.insert(perm);
        }
    }

    /** 设置数据权限范围(upsert)。 */
    @Transactional
    public void setDataScope(Long roleId, String scopeType, String orgIds) {
        checkRole(roleId);
        if (!List.of("all", "current", "children").contains(scopeType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "scopeType 仅支持 all/current/children");
        }
        SysDataScope scope = dataScopeMapper.selectOne(Wrappers.lambdaQuery(SysDataScope.class)
                .eq(SysDataScope::getRoleId, roleId));
        if (scope == null) {
            scope = new SysDataScope();
            scope.setRoleId(roleId);
            scope.setScopeType(scopeType);
            scope.setOrgIds(orgIds);
            dataScopeMapper.insert(scope);
        } else {
            scope.setScopeType(scopeType);
            scope.setOrgIds(orgIds);
            dataScopeMapper.updateById(scope);
        }
    }

    private void checkRole(Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
    }

    private RoleVO toVO(SysRole role) {
        List<String> perms = rolePermMapper.selectList(Wrappers.lambdaQuery(SysRolePerm.class)
                        .eq(SysRolePerm::getRoleId, role.getId()))
                .stream().map(SysRolePerm::getPermCode).toList();
        SysDataScope scope = dataScopeMapper.selectOne(Wrappers.lambdaQuery(SysDataScope.class)
                .eq(SysDataScope::getRoleId, role.getId()));
        return new RoleVO(role.getId(), role.getCode(), role.getName(), role.getRemark(),
                perms, scope == null ? null : scope.getScopeType(),
                scope == null ? null : scope.getOrgIds());
    }
}
