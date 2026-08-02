package com.examflow.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.examflow.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考试会话表(exam_session,见 TDD §4.2.4)。
 * 核心高并发表:生产环境按 registration_id 分片(16 库 × 64 表)。
 * question_ids 为抽题结果(加密存储),seed 保证抽题可复现(TDD §6.3)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_session")
public class ExamSession extends BaseEntity {

    /** 会话号 */
    private String sessionNo;

    /** 报名记录 ID(分片键) */
    private Long registrationId;

    /** 场次 ID */
    private Long slotId;

    /** 试卷快照 ID */
    private Long paperSnapshotId;

    /** 抽题种子 SHA256(paperSnapshotId + slotId + candidateId) */
    private String seed;

    /** 抽题结果题目序列(JSON,加密) */
    private String questionIds;

    /** 状态:ANSWERING/SUBMITTED/GRADING/GRADED/CLOSED/VOID */
    private String status;

    /** 作答开始时间(以服务器时间 UTC+8 为准) */
    private LocalDateTime startedAt;

    /** 截止时间(服务器时间) */
    private LocalDateTime deadlineAt;

    /** 已进入次数(上限可配,默认 3) */
    private Integer enterCount;

    /** 服务端已确认的作答序号(seq 对齐机制) */
    private Long lastSeq;

    /** 交卷时间 */
    private LocalDateTime submitTime;

    /** 客户端 IP / 设备指纹(监考用) */
    private String clientIp;
    private String deviceFp;
}
