package com.examflow.user.dto;

import java.util.List;

/**
 * 角色视图:含权限码集合与数据权限范围。
 */
public record RoleVO(Long id, String code, String name, String remark,
                     List<String> permCodes, String scopeType, String scopeOrgIds) {
}
