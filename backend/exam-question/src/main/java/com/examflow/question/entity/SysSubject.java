package com.examflow.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 科目(sys_subject)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_subject")
public class SysSubject extends BaseEntity {

    /** 科目编码 */
    private String code;

    /** 科目名称 */
    private String name;

    /** enabled/disabled */
    private String status;
}
