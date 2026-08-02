package com.examflow.auth.dto;

/**
 * 认证所需用户信息(user-service 内部接口返回)。
 */
public record UserInfo(Long userId, String username, String name, String passwordHash,
                       String status, String userType, Long orgId) {
}
