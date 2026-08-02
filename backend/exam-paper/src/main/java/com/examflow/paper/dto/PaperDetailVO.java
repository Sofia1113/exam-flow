package com.examflow.paper.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷详情/预览。
 * fixed:questions 为卷面题目(含解密答案,withAnswer 控制);
 * strategy:blueprint 为组卷蓝图,questions 为预览抽题示意。
 */
public record PaperDetailVO(PaperVO paper, String blueprint,
                            List<PreviewQuestion> questions) {

    public record PreviewQuestion(Integer seq, String type, String stem, String options,
                                  String answer, String analysis, BigDecimal score,
                                  Integer shuffleGroup) {
    }
}
