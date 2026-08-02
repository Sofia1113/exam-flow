package com.examflow.user.util;

/**
 * 敏感信息脱敏展示(身份证/手机号存储加密,对外一律脱敏)。
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /** 手机号:138****0000。 */
    public static String phone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return phone;
        }
        return digits.substring(0, 3) + "****" + digits.substring(7);
    }

    /** 身份证号:前 3 后 4。 */
    public static String idCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return null;
        }
        String digits = idCard.replaceAll("\\s", "");
        if (digits.length() < 8) {
            return idCard;
        }
        return digits.substring(0, 3) + "**********" + digits.substring(digits.length() - 4);
    }
}
