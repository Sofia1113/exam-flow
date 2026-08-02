package com.examflow.grading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话只读视图(共享库,判分需要试卷快照与状态)。
 */
@Data
@TableName("exam_session")
public class ExamSessionView {

    private Long id;
    private Long registrationId;
    private Long paperSnapshotId;
    private String status;
    private LocalDateTime submitTime;
}
