package com.examflow.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织树(sys_org,见 TDD §4.2.1):path 为层级路径(如 /1/2/3)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseEntity {

    /** 父组织 ID,0 为根 */
    private Long parentId;

    /** 组织名称 */
    private String name;

    /** 层级路径 /1/2/3 */
    private String path;

    /** 类型:unit/dept/team */
    private String orgType;

    /** enabled/disabled */
    private String status;
}
