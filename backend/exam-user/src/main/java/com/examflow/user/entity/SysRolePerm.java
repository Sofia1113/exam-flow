package com.examflow.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-权限关联(sys_role_perm):perm_code 为菜单/操作权限编码。
 */
@Data
@TableName("sys_role_perm")
public class SysRolePerm {

    private Long id;
    private Long roleId;

    /** 权限编码,如 user:create / question:audit / menu:dashboard */
    private String permCode;
}
