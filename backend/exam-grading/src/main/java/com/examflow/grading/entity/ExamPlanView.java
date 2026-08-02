package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 考试计划只读视图(共享库,通知模板需要考试名称)。
 */
@Data
@TableName("exam_plan")
public class ExamPlanView {

    private Long id;
    private String name;
    private String status;
}
