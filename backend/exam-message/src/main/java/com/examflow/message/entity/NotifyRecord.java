package com.examflow.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 通知记录(notify_record):渠道/模板/回执,失败重试。
 */
@Data
@TableName("notify_record")
public class NotifyRecord {

    private Long id;

    /** sms/site/email */
    private String channel;

    private String templateCode;

    /** 手机号/用户 ID/邮箱 */
    private String target;

    private String content;

    /** pending/success/failed */
    private String status;

    private Integer retryCount;

    private String receipt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
