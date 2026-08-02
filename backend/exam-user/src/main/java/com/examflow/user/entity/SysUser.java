package com.examflow.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表(sys_user,见 TDD §4.2.1)。
 * 身份证/手机号生产环境按 AesUtil 加密存储,脱敏展示。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录账号 */
    private String username;

    /** bcrypt 哈希(PasswordUtil) */
    private String passwordHash;

    /** 姓名 */
    private String name;

    /** 手机号(加密) */
    private String phone;

    /** 身份证号(加密) */
    private String idCard;

    /** 所属组织 */
    private Long orgId;

    /** 账号类型:internal=内部员工 / external=社会考生 */
    private String userType;

    /** 状态:enabled / disabled / locked */
    private String status;
}
