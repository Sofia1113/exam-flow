package com.examflow.user.dto;

/**
 * 内部接口返回的用户信息(含口令哈希,仅限服务间调用,不经过网关)。
 */
public record UserInfo(Long userId, String username, String name, String passwordHash,
                       String status, String userType, Long orgId) {
}
