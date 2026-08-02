package com.examflow.common.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码。
 * 分段约定(见 TDD §5.1):
 * 0 成功;10xxx 通用;11xxx 认证;12xxx 权限;13xxx 参数;14xxx 业务;15xxx 第三方;5xxxx 系统。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "成功"),

    // 通用 10xxx
    SYSTEM_ERROR(50000, "系统繁忙,请稍后重试"),
    UNIMPLEMENTED(10001, "功能开发中"),
    NOT_FOUND(10002, "资源不存在"),

    // 认证 11xxx
    UNAUTHORIZED(11001, "未登录或会话已过期"),
    LOGIN_FAILED(11002, "账号或密码错误"),
    ACCOUNT_LOCKED(11003, "账号已锁定,请稍后再试"),
    SMS_CODE_ERROR(11004, "短信验证码错误或已过期"),
    TOKEN_INVALID(11005, "令牌无效"),

    // 权限 12xxx
    FORBIDDEN(12001, "无权限执行该操作"),
    SENSITIVE_OP_VERIFY_REQUIRED(12002, "敏感操作需二次验证"),

    // 参数 13xxx
    PARAM_ERROR(13001, "参数错误"),
    VALIDATE_FAILED(13002, "参数校验不通过"),

    // 业务 14xxx
    BIZ_ERROR(14000, "业务处理失败"),
    EXAM_LATE(14001, "已迟到,禁止入场"),
    EXAM_SESSION_SUBMITTED(14002, "会话已交卷或已作废"),
    EXAM_SESSION_LOCKED(14003, "会话锁定,请稍后重试"),
    EXAM_ENTER_LIMIT(14004, "进入次数超限,请联系监考员"),
    EXAM_SEQ_OUTDATED(14005, "增量序号落后,请按服务端序号重发"),
    EXAM_SUBMIT_CONFLICT(14006, "交卷处理中,请稍后查询结果"),
    EXAM_NOT_STARTED(14007, "考试尚未开始"),
    EXAM_ALREADY_CLOSED(14008, "考试已结束"),

    // 第三方 15xxx
    THIRD_PARTY_ERROR(15001, "第三方服务暂不可用"),
    SMS_SEND_FAILED(15002, "短信发送失败");

    private final int code;
    private final String message;
}
