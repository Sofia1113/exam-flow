package com.examflow.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解:标注在需要留痕的接口方法上。
 * 生产落地:切面异步写入独立只追加存储的 audit_log 表(禁 update/delete,见 TDD §7.5)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 业务模块,如 exam / score / paper。 */
    String module();

    /** 动作描述,如 "交卷" / "成绩更正"。 */
    String action();
}
