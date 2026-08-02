package com.examflow.message.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.message.entity.NotifyRecord;
import com.examflow.message.entity.NotifyTemplate;
import com.examflow.message.mapper.NotifyRecordMapper;
import com.examflow.message.mapper.NotifyTemplateMapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息服务(FR-MSG):模板渲染、多渠道发送(短信当前日志模拟)、失败标记、
 * 站内信必达。批量触达与失败重试由 XXL-JOB 补偿(生产)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final NotifyTemplateMapper templateMapper;
    private final NotifyRecordMapper recordMapper;

    /** 发送:channel 为空按模板默认渠道;站内信 target 为用户 ID。 */
    public void send(String templateCode, String target, String channel, Map<String, Object> params) {
        NotifyTemplate template = templateMapper.selectOne(Wrappers.lambdaQuery(NotifyTemplate.class)
                .eq(NotifyTemplate::getCode, templateCode)
                .eq(NotifyTemplate::getStatus, "enabled"));
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知模板不存在或已停用: " + templateCode);
        }
        String resolvedChannel = channel == null || channel.isBlank() ? template.getChannel() : channel;
        String content = render(template.getContent(), params);
        NotifyRecord record = new NotifyRecord();
        record.setChannel(resolvedChannel);
        record.setTemplateCode(templateCode);
        record.setTarget(target);
        record.setContent(content);
        record.setStatus("pending");
        record.setRetryCount(0);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);
        try {
            switch (resolvedChannel) {
                case "sms" -> sendSms(target, content);
                case "site" -> {
                    // 站内信:记录即达(查询按 target=用户 ID)
                    record.setReceipt("site-delivered");
                }
                case "email" -> sendEmail(target, template.getTitle(), content);
                default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "未知渠道: " + resolvedChannel);
            }
            record.setStatus("success");
        } catch (Exception e) {
            // 失败标记,生产由补偿任务重试 3 次(FR-MSG-04)
            log.error("通知发送失败: template={}, target={}", templateCode, target, e);
            record.setStatus("failed");
        }
        record.setUpdateTime(LocalDateTime.now());
        recordMapper.updateById(record);
    }

    /** 站内信列表(按当前用户)。 */
    public java.util.List<NotifyRecord> myNotifications(String userId) {
        return recordMapper.selectList(Wrappers.lambdaQuery(NotifyRecord.class)
                .eq(NotifyRecord::getChannel, "site")
                .eq(NotifyRecord::getTarget, userId)
                .orderByDesc(NotifyRecord::getId));
    }

    /** 占位符渲染:${key} → params[key]。 */
    private String render(String content, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return content;
        }
        String result = content;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}",
                    entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return result;
    }

    /** 短信:当前日志模拟(生产接入短信通道,双供应商容灾)。 */
    private void sendSms(String phone, String content) {
        log.info("[短信模拟] → {}: {}", phone, content);
    }

    private void sendEmail(String to, String title, String content) {
        // 生产:JavaMailSender 发送;骨架为日志模拟
        log.info("[邮件模拟] → {}: {} - {}", to, title, content);
    }
}
