package com.examflow.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 考试计划只读视图(共享库,取试卷关联)。
 */
@Data
@TableName("exam_plan")
public class ExamPlanView {

    private Long id;
    private Long subjectId;
    private Long paperId;
    private String status;
}
