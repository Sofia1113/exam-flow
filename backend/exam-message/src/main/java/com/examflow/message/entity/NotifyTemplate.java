package com.examflow.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 通知模板(notify_template):占位符 ${key} 渲染。
 */
@Data
@TableName("notify_template")
public class NotifyTemplate {

    private Long id;

    /** 模板编码,如 reg_approved */
    private String code;

    /** sms/site/email */
    private String channel;

    private String title;

    /** 正文(含 ${name} 等占位符) */
    private String content;

    /** enabled/disabled */
    private String status;
}
