package com.examflow.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色(sys_role):code 唯一(如 SYS_ADMIN/EXAM_ADMIN/CANDIDATE)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色编码,唯一 */
    private String code;

    /** 角色名称 */
    private String name;

    /** 备注 */
    private String remark;
}
