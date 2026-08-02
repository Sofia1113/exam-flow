package com.examflow.registration.dto;

/**
 * 报名资格校验所需用户信息。
 */
public record UserInfo(Long userId, String username, String name, String passwordHash,
                       String status, String userType, Long orgId) {
}
