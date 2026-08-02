package com.examflow.grading.dto;

/**
 * 用户信息(成绩导出)。
 */
public record UserInfo(Long userId, String username, String name, String passwordHash,
                       String status, String userType, Long orgId) {
}
