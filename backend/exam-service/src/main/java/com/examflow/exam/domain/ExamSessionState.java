package com.examflow.exam.domain;

import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;

/**
 * 考试会话状态机(见 TDD §3)。
 *
 * <pre>
 * ANSWERING --提交--> SUBMITTED --进入评阅--> GRADING --判分完成--> GRADED --归档--> CLOSED
 *      |_________________作废___________________|
 * </pre>
 */
public enum ExamSessionState {

    ANSWERING("作答中"),
    SUBMITTED("已交卷"),
    GRADING("评阅中"),
    GRADED("已出分"),
    CLOSED("已归档"),
    VOID("已作废");

    private final String desc;

    ExamSessionState(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /** 是否允许提交。 */
    public boolean canSubmit() {
        return this == ANSWERING;
    }

    /** 是否允许继续作答(进入/恢复/保存)。 */
    public boolean canAnswer() {
        return this == ANSWERING;
    }

    /** 状态迁移:提交。非法迁移抛出业务异常。 */
    public ExamSessionState submit() {
        if (!canSubmit()) {
            throw new BusinessException(ErrorCode.EXAM_SESSION_SUBMITTED,
                    "当前状态(" + desc + ")不允许交卷");
        }
        return SUBMITTED;
    }

    /** 状态迁移:作废。 */
    public ExamSessionState voidOut() {
        if (this == CLOSED) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "已归档会话不可作废");
        }
        return VOID;
    }
}
