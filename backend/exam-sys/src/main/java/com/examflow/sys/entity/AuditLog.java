package com.examflow.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审计日志表(audit_log,见 TDD §4.2.6)。
 * 特殊约定:
 * - 独立存储,库权限禁止 update/delete,只追加;
 * - 留存 ≥ 5 年,到期归档离线存储;
 * - 由 sys-service 消费审计事件写入,业务服务不直连本表。
 */
@Data
@TableName("audit_log")
public class AuditLog {

    private Long id;
    private String operatorId;
    private String action;
    private String module;
    private String objectType;
    private String objectId;
    private String before;
    private String after;
    private String ip;
    private Boolean success;
    private String traceId;
    private LocalDateTime createTime;
}
