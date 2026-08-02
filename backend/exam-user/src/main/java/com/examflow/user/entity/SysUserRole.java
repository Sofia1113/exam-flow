package com.examflow.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关联(sys_user_role)。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    private Long id;
    private Long userId;
    private Long roleId;
}
