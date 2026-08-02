package com.examflow.paper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 试卷快照(paper_snapshot,不可变):发布即固化。
 * content 为整卷加密 JSON;题目内容快照在 paper_question(逐题)。
 */
@Data
@TableName("paper_snapshot")
public class PaperSnapshot {

    private Long id;
    private Long paperId;

    /** 快照编号,如 SNAP-20260801-001 */
    private String snapshotNo;

    /** 整卷内容(AES 加密 JSON:名称/总分/及格线/时长/蓝图) */
    private String content;

    private java.math.BigDecimal totalScore;

    private Integer version;

    /** active/archived */
    private String status;

    private LocalDateTime createTime;
}
