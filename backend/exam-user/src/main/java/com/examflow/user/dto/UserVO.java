package com.examflow.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图(脱敏展示,不含口令/密文字段)。
 */
public record UserVO(Long id, String username, String name, String phoneMasked,
                     Long orgId, String orgName, String userType, String status,
                     List<String> roles, LocalDateTime createTime) {
}
