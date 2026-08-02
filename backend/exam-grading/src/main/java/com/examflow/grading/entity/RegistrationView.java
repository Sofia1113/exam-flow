package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 报名记录只读视图(共享库,按考次定位会话)。
 */
@Data
@TableName("exam_registration")
public class RegistrationView {

    private Long id;
    private Long planId;
    private Long userId;
    private String status;
}
