package com.examflow.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.util.PasswordUtil;
import com.examflow.user.dto.CurrentUserPerm;
import com.examflow.user.dto.UserVO;
import com.examflow.user.entity.SysOrg;
import com.examflow.user.entity.SysRole;
import com.examflow.user.entity.SysUser;
import com.examflow.user.entity.SysUserRole;
import com.examflow.user.mapper.DataScopeMapper;
import com.examflow.user.mapper.OrgMapper;
import com.examflow.user.mapper.RoleMapper;
import com.examflow.user.mapper.RolePermMapper;
import com.examflow.user.mapper.UserMapper;
import com.examflow.user.mapper.UserRoleMapper;
import com.examflow.user.util.DesensitizeUtil;
import com.examflow.common.context.UserContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务:分页(数据权限隔离)、创建/更新/状态、角色分配、当前用户权限。
 *
 * <p>数据权限(FR-ORG-02):按当前用户角色的数据范围过滤可见组织 ——
 * all=全部 / current=本级 / children=本级及下级(path LIKE 匹配)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final OrgMapper orgMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermMapper rolePermMapper;
    private final DataScopeMapper dataScopeMapper;

    /** 用户分页:自动叠加当前用户数据权限范围。 */
    public PageResult<UserVO> page(long page, long size, Long orgId, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class);
        List<Long> visibleOrgs = visibleOrgIds(UserContext.requireUserId());
        if (visibleOrgs != null) {
            wrapper.in(SysUser::getOrgId, visibleOrgs);
        }
        if (orgId != null) {
            wrapper.eq(SysUser::getOrgId, orgId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getName, keyword));
        }
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.convert(this::toVO));
    }

    public UserVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    /** 创建用户:用户名唯一、密码 bcrypt、可选分配角色。 */
    @Transactional
    public Long create(SysUser user, List<Long> roleIds) {
        Long exists = userMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, user.getUsername()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "用户名已存在");
        }
        user.setPasswordHash(PasswordUtil.encode(user.getPasswordHash()));
        user.setStatus("enabled");
        userMapper.insert(user);
        assignRoles(user.getId(), roleIds);
        log.info("创建用户: id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    /** 更新基本信息与角色。 */
    @Transactional
    public void update(SysUser user, List<Long> roleIds) {
        SysUser exist = userMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            exist.setPasswordHash(PasswordUtil.encode(user.getPasswordHash()));
        }
        if (user.getName() != null) {
            exist.setName(user.getName());
        }
        if (user.getPhone() != null) {
            exist.setPhone(user.getPhone());
        }
        if (user.getOrgId() != null) {
            exist.setOrgId(user.getOrgId());
        }
        userMapper.updateById(exist);
        if (roleIds != null) {
            assignRoles(user.getId(), roleIds);
        }
    }

    /** 启用/停用账号。 */
    @Transactional
    public void changeStatus(Long id, String status) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        exist.setStatus(status);
        userMapper.updateById(exist);
        // TODO: 停用时撤销其全部令牌(调用 auth 强制下线,见 X-001)
    }

    /** 分配角色(全量替换)。 */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRole.class)
                .eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    /** 当前用户权限(前端菜单/操作控制)。 */
    public CurrentUserPerm currentPerm() {
        Long userId = UserContext.requireUserId();
        List<SysRole> roles = rolesOf(userId);
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        // 超级管理员默认全部权限(SYS_ADMIN 不做 perm 表校验)
        boolean isSuper = roles.stream().anyMatch(r -> "SYS_ADMIN".equals(r.getCode()));
        List<String> perms = isSuper ? List.of("*")
                : roleIds.isEmpty() ? List.of() : rolePermsOf(roleIds);
        List<String> scopeTypes = roleIds.isEmpty() ? List.of()
                : dataScopeMapper.selectList(Wrappers.lambdaQuery(com.examflow.user.entity.SysDataScope.class)
                        .in(com.examflow.user.entity.SysDataScope::getRoleId, roleIds))
                .stream().map(com.examflow.user.entity.SysDataScope::getScopeType).toList();
        return new CurrentUserPerm(userId, roles.stream().map(SysRole::getCode).toList(),
                perms, scopeTypes);
    }

    /** 指定用户的有效权限码集合(内部接口用):SYS_ADMIN 返回 ["*"]。 */
    public List<String> getUserPerms(Long userId) {
        List<SysRole> roles = rolesOf(userId);
        if (roles.isEmpty()) {
            return List.of();
        }
        boolean isSuper = roles.stream().anyMatch(r -> "SYS_ADMIN".equals(r.getCode()));
        if (isSuper) {
            return List.of("*");
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        return rolePermsOf(roleIds);
    }

    /** 当前用户可见组织范围:null 表示全部(不限制)。 */
    private List<Long> visibleOrgIds(Long userId) {
        List<SysRole> roles = rolesOf(userId);
        if (roles.isEmpty()) {
            return List.of(); // 无角色 → 无数据
        }
        boolean all = roles.stream().anyMatch(r -> {
            var scope = dataScopeMapper.selectOne(Wrappers.lambdaQuery(
                    com.examflow.user.entity.SysDataScope.class)
                    .eq(com.examflow.user.entity.SysDataScope::getRoleId, r.getId()));
            return scope != null && "all".equals(scope.getScopeType());
        });
        if (all) {
            return null;
        }
        // 取并集:current = 自身组织;children = 自身 + 下级(path 前缀)
        List<Long> orgIds = new java.util.ArrayList<>();
        SysUser self = userMapper.selectById(userId);
        for (SysRole role : roles) {
            var scope = dataScopeMapper.selectOne(Wrappers.lambdaQuery(
                    com.examflow.user.entity.SysDataScope.class)
                    .eq(com.examflow.user.entity.SysDataScope::getRoleId, role.getId()));
            if (scope == null || self == null || self.getOrgId() == null) {
                continue;
            }
            if ("current".equals(scope.getScopeType())) {
                orgIds.add(self.getOrgId());
            } else if ("children".equals(scope.getScopeType())) {
                SysOrg org = orgMapper.selectById(self.getOrgId());
                if (org != null) {
                    orgIds.addAll(orgMapper.selectList(Wrappers.lambdaQuery(SysOrg.class)
                                    .likeRight(SysOrg::getPath, org.getPath()))
                            .stream().map(SysOrg::getId).toList());
                }
            }
        }
        return orgIds.stream().distinct().toList();
    }

    private List<SysRole> rolesOf(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(SysUserRole.class)
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    private List<String> rolePermsOf(List<Long> roleIds) {
        return rolePermMapper.selectList(Wrappers.lambdaQuery(com.examflow.user.entity.SysRolePerm.class)
                        .in(com.examflow.user.entity.SysRolePerm::getRoleId, roleIds))
                .stream().map(com.examflow.user.entity.SysRolePerm::getPermCode)
                .distinct().toList();
    }

    private UserVO toVO(SysUser user) {
        String orgName = user.getOrgId() == null ? null
                : orgMapper.selectById(user.getOrgId()) == null ? null
                : orgMapper.selectById(user.getOrgId()).getName();
        List<String> roles = rolesOf(user.getId()).stream().map(SysRole::getCode).toList();
        return new UserVO(user.getId(), user.getUsername(), user.getName(),
                DesensitizeUtil.phone(user.getPhone()), user.getOrgId(), orgName,
                user.getUserType(), user.getStatus(), roles, user.getCreateTime());
    }
}
