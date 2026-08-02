package com.examflow.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.user.dto.OrgNode;
import com.examflow.user.entity.SysOrg;
import com.examflow.user.mapper.OrgMapper;
import com.examflow.user.mapper.UserMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 组织树服务:树查询、CRUD、path 层级路径维护(数据权限隔离依赖 path LIKE 匹配)。
 */
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgMapper orgMapper;
    private final UserMapper userMapper;

    /** 全量组织树。 */
    public List<OrgNode> tree() {
        List<SysOrg> all = orgMapper.selectList(Wrappers.lambdaQuery(SysOrg.class)
                .eq(SysOrg::getStatus, "enabled")
                .orderByAsc(SysOrg::getId));
        return buildTree(all);
    }

    /** 创建组织:维护 path = 父.path + "/" + id。 */
    @Transactional
    public Long create(SysOrg org) {
        if (org.getParentId() == null || org.getParentId() == 0) {
            org.setParentId(0L);
        } else {
            SysOrg parent = orgMapper.selectById(org.getParentId());
            if (parent == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "父组织不存在");
            }
        }
        org.setStatus(org.getStatus() == null ? "enabled" : org.getStatus());
        orgMapper.insert(org);
        // 插入后回填 path(依赖自增 id,故两次写)
        org.setPath(parentPath(org) + org.getId());
        orgMapper.updateById(org);
        return org.getId();
    }

    /** 更新组织(名称/类型;不可更换父组织,避免 path 链重建)。 */
    @Transactional
    public void update(SysOrg org) {
        SysOrg exist = orgMapper.selectById(org.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在");
        }
        if (org.getParentId() != null && !exist.getParentId().equals(org.getParentId())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "不允许更换父组织,请新建组织后迁移");
        }
        exist.setName(org.getName());
        exist.setOrgType(org.getOrgType());
        if (org.getStatus() != null) {
            exist.setStatus(org.getStatus());
        }
        orgMapper.updateById(exist);
    }

    /** 删除组织:存在子组织或组织下有用例时拒绝。 */
    @Transactional
    public void delete(Long id) {
        Long children = orgMapper.selectCount(Wrappers.lambdaQuery(SysOrg.class)
                .eq(SysOrg::getParentId, id));
        if (children > 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "存在子组织,无法删除");
        }
        Long users = userMapper.selectCount(Wrappers.lambdaQuery(com.examflow.user.entity.SysUser.class)
                .eq(com.examflow.user.entity.SysUser::getOrgId, id));
        if (users > 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "组织下存在用户,无法删除");
        }
        orgMapper.deleteById(id);
    }

    /** 计算某组织的可见下级组织 ID(含自身),数据权限 children 用。 */
    public List<Long> selfAndDescendantIds(Long orgId) {
        SysOrg org = orgMapper.selectById(orgId);
        if (org == null) {
            return List.of();
        }
        return orgMapper.selectList(Wrappers.lambdaQuery(SysOrg.class)
                        .likeRight(SysOrg::getPath, org.getPath()))
                .stream().map(SysOrg::getId).toList();
    }

    private String parentPath(SysOrg org) {
        if (org.getParentId() == 0) {
            return "/";
        }
        SysOrg parent = orgMapper.selectById(org.getParentId());
        return parent == null ? "/" : parent.getPath() + "/";
    }

    private List<OrgNode> buildTree(List<SysOrg> all) {
        Map<Long, List<SysOrg>> byParent = all.stream()
                .collect(Collectors.groupingBy(SysOrg::getParentId));
        // 注意:getOrDefault 默认值 List.of() 为不可变列表,不可 sort,须复制
        List<SysOrg> roots = new ArrayList<>(byParent.getOrDefault(0L, List.of()));
        roots.sort(Comparator.comparing(SysOrg::getId));
        List<OrgNode> tree = new ArrayList<>();
        for (SysOrg root : roots) {
            tree.add(toNode(root, byParent));
        }
        return tree;
    }

    private OrgNode toNode(SysOrg org, Map<Long, List<SysOrg>> byParent) {
        List<OrgNode> children = new ArrayList<>();
        List<SysOrg> subs = new ArrayList<>(byParent.getOrDefault(org.getId(), List.of()));
        subs.sort(Comparator.comparing(SysOrg::getId));
        for (SysOrg sub : subs) {
            children.add(toNode(sub, byParent));
        }
        return new OrgNode(org.getId(), org.getParentId(), org.getName(),
                org.getPath(), org.getOrgType(), org.getStatus(), children);
    }
}
