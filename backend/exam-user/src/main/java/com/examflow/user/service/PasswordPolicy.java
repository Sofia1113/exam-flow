package com.examflow.user.service;

import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;

/**
 * 口令安全策略(FR-AUTH-02, TDD §7.1):
 * 长度 ≥ 10,且大小写字母、数字、符号至少三类。
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String raw) {
        if (raw == null || raw.length() < 10) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "密码长度至少 10 位");
        }
        int kinds = 0;
        if (raw.matches(".*[a-z].*")) {
            kinds++;
        }
        if (raw.matches(".*[A-Z].*")) {
            kinds++;
        }
        if (raw.matches(".*\\d.*")) {
            kinds++;
        }
        if (raw.matches(".*[^a-zA-Z0-9].*")) {
            kinds++;
        }
        if (kinds < 3) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "密码需包含大写字母、小写字母、数字、符号中至少三类");
        }
    }
}
