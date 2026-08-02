package com.examflow.user.dto;

import java.util.List;

/**
 * 当前用户权限视图(前端菜单/操作控制依据,PRD FR-ORG-02)。
 */
public record CurrentUserPerm(Long userId, List<String> roles, List<String> permCodes,
                              List<String> dataScopeTypes) {
}
