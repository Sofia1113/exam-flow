package com.examflow.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试接口 DTO:进入/恢复/保存返回。
 */
public final class ExamDTO {

    private ExamDTO() {
    }

    /** 下发考生题目(不含答案)。 */
    public record ClientQuestion(Integer seq, String type, String stem, String options,
                                 BigDecimal score) {
    }

    /** 进入考试返回。 */
    public record EnterResp(Long sessionId, LocalDateTime deadlineAt, Integer durationMin,
                            List<ClientQuestion> questions) {
    }

    /** 恢复会话返回:已存答案(解密)+ 剩余时间(服务器计算)。 */
    public record ResumeResp(Long sessionId, String status, LocalDateTime deadlineAt,
                             long remainSeconds, Long lastSeq,
                             List<ClientQuestion> questions, List<SavedAnswer> answers) {
    }

    /** 已保存的作答(考生答案明文)。 */
    public record SavedAnswer(Integer questionSeq, String answer) {
    }
}
