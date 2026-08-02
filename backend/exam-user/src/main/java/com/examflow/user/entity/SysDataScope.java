package com.examflow.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 数据权限范围(sys_data_scope):角色 × 组织范围。
 * scope_type:all=全部 / current=本级 / children=本级及下级。
 */
@Data
@TableName("sys_data_scope")
public class SysDataScope {

    private Long id;
    private Long roleId;

    /** all/current/children */
    private String scopeType;

    /** 限定组织 ID 列表(JSON,可选) */
    private String orgIds;
}
