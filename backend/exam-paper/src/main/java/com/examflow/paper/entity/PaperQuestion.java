package com.examflow.paper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 快照题目明细(paper_question):题目内容快照(加密 JSON)+ 卷面位置。
 * shuffle_group 同组可随机打乱(确定性乱序,0=固定)。
 */
@Data
@TableName("paper_question")
public class PaperQuestion {

    private Long id;
    private Long snapshotId;

    /** 题目内容快照(AES 加密 JSON,含题干/选项/答案/解析) */
    private String questionSnapshot;

    /** 卷面序号 */
    private Integer seq;

    private java.math.BigDecimal score;

    /** 乱序组:同组题目可随机打乱,0=固定 */
    private Integer shuffleGroup;
}
