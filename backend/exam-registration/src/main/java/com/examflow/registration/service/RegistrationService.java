package com.examflow.registration.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examflow.common.context.UserContext;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.registration.client.UserServiceClient;
import com.examflow.registration.dto.UserInfo;
import com.examflow.registration.entity.ExamPlan;
import com.examflow.registration.entity.ExamRegistration;
import com.examflow.registration.entity.ExamSlot;
import com.examflow.registration.mapper.ExamPlanMapper;
import com.examflow.registration.mapper.ExamRegistrationMapper;
import com.examflow.registration.mapper.ExamSlotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报名排考服务(FR-REG/FR-SCHED):考试计划状态机、报名自动预审 + 人工审核、
 * 名额控制、准考证生成、场次机位分配与冲突检测、名单导出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final ExamPlanMapper planMapper;
    private final ExamRegistrationMapper registrationMapper;
    private final ExamSlotMapper slotMapper;
    private final UserServiceClient userClient;
    private final com.examflow.registration.client.MessageServiceClient messageClient;
    private final ObjectMapper objectMapper;

    // ---------- 考试计划(FR-REG-01) ----------

    public PageResult<ExamPlan> plans(long page, long size, String status) {
        var wrapper = Wrappers.lambdaQuery(ExamPlan.class);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ExamPlan::getStatus, status);
        }
        wrapper.orderByDesc(ExamPlan::getId);
        return PageResult.of(planMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public ExamPlan planDetail(Long id) {
        return requirePlan(id);
    }

    @Transactional
    public Long createPlan(PlanReq req) {
        if (req.name() == null || req.name().isBlank() || req.subjectId() == null
                || req.regStart() == null || req.regEnd() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "计划名称/科目/报名时间窗必填");
        }
        if (req.regEnd().isBefore(req.regStart())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "报名截止时间早于开始时间");
        }
        ExamPlan plan = new ExamPlan();
        plan.setName(req.name());
        plan.setSubjectId(req.subjectId());
        plan.setPaperId(req.paperId());
        plan.setRegStart(req.regStart());
        plan.setRegEnd(req.regEnd());
        plan.setExamDate(req.examDate() == null
                ? req.regEnd().toLocalDate().atStartOfDay() : req.examDate());
        plan.setCapacity(req.capacity() == null ? 0 : req.capacity());
        plan.setConditionRule(req.conditionRule());
        plan.setStatus("draft");
        planMapper.insert(plan);
        log.info("创建考试计划: id={}, name={}", plan.getId(), plan.getName());
        return plan.getId();
    }

    /** 送审:draft → pending。 */
    @Transactional
    public void submitPlan(Long id) {
        ExamPlan plan = requirePlan(id);
        if (!"draft".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅草稿状态可送审");
        }
        plan.setStatus("pending");
        planMapper.updateById(plan);
    }

    /** 审批:待审 → 已审(通过)/草稿(驳回)。 */
    @Transactional
    public void auditPlan(Long id, boolean pass, String opinion) {
        ExamPlan plan = requirePlan(id);
        if (!"pending".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅待审状态可审核");
        }
        plan.setStatus(pass ? "approved" : "draft");
        plan.setApproverId(UserContext.requireUserId());
        plan.setApproveTime(LocalDateTime.now());
        plan.setApproveOpinion(opinion);
        planMapper.updateById(plan);
        log.info("考试计划审批: id={}, pass={}", id, pass);
    }

    // ---------- 报名(FR-REG-02~06) ----------

    /** 考生报名:窗口校验 + 条件自动预审 + 名额控制 + 唯一约束。 */
    @Transactional
    public Long apply(Long userId, Long planId, Long slotId) {
        ExamPlan plan = requirePlan(planId);
        if (!"approved".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "考试计划未开放报名");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(plan.getRegStart()) || now.isAfter(plan.getRegEnd())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "当前不在报名时间窗口内");
        }
        Long exists = registrationMapper.selectCount(Wrappers.lambdaQuery(ExamRegistration.class)
                .eq(ExamRegistration::getPlanId, planId)
                .eq(ExamRegistration::getUserId, userId));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "您已报名该考试");
        }
        // 名额控制(并发由 uk(plan_id,user_id) 与容量检查兜底)
        if (plan.getCapacity() != null && plan.getCapacity() > 0) {
            Long count = registrationMapper.selectCount(Wrappers.lambdaQuery(ExamRegistration.class)
                    .eq(ExamRegistration::getPlanId, planId)
                    .notIn(ExamRegistration::getStatus, "rejected", "withdrawn"));
            if (count >= plan.getCapacity()) {
                throw new BusinessException(ErrorCode.BIZ_ERROR, "名额已满");
            }
        }
        UserInfo user = userClient.getUser(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        ExamRegistration reg = new ExamRegistration();
        reg.setPlanId(planId);
        reg.setUserId(userId);
        reg.setSlotId(slotId);
        reg.setWaitlist(0);
        // 条件规则自动预审(FR-REG-02):命中规则 → 直接通过并发放准考证
        String autoStatus = autoAudit(plan, user);
        reg.setStatus(autoStatus);
        if ("approved".equals(autoStatus)) {
            reg.setAuditTime(LocalDateTime.now());
        }
        registrationMapper.insert(reg);
        if ("approved".equals(autoStatus)) {
            reg.setTicketNo(genTicketNo(planId, reg.getId()));
            registrationMapper.updateById(reg);
            notifyApproved(plan, reg, user);
        }
        log.info("报名提交: plan={}, user={}, 自动预审={}", planId, userId, autoStatus);
        return reg.getId();
    }

    /** 审核通过通知(FR-MSG-03):短信 + 站内信。 */
    private void notifyApproved(ExamPlan plan, ExamRegistration reg, UserInfo user) {
        try {
            Map<String, Object> params = Map.of("name", user.name(), "examName", plan.getName(),
                    "ticketNo", reg.getTicketNo() == null ? "" : reg.getTicketNo());
            if (user.phone() != null) {
                messageClient.send(new com.examflow.registration.client.MessageServiceClient.SendReq(
                        "reg_approved", user.phone(), "sms", params));
            }
            messageClient.send(new com.examflow.registration.client.MessageServiceClient.SendReq(
                    "reg_approved", String.valueOf(user.userId()), "site", params));
        } catch (Exception e) {
            log.warn("报名通知发送失败(不影响主流程): reg={}", reg.getId(), e);
        }
    }

    /** 人工审核:通过发放准考证,驳回释放名额(FR-REG-04)。 */
    @Transactional
    public void auditRegistration(Long id, boolean pass, String opinion) {
        ExamRegistration reg = requireRegistration(id);
        if (!"pending".equals(reg.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅待审状态可审核");
        }
        reg.setStatus(pass ? "approved" : "rejected");
        reg.setAuditBy(UserContext.requireUserId());
        reg.setAuditOpinion(opinion);
        reg.setAuditTime(LocalDateTime.now());
        if (pass) {
            reg.setTicketNo(genTicketNo(reg.getPlanId(), reg.getId()));
        }
        registrationMapper.updateById(reg);
        // 审核结果通知
        try {
            UserInfo user = userClient.getUser(reg.getUserId());
            ExamPlan plan = planMapper.selectById(reg.getPlanId());
            if (user != null && plan != null) {
                Map<String, Object> params = Map.of("name", user.name(), "examName", plan.getName(),
                        "ticketNo", reg.getTicketNo() == null ? "" : reg.getTicketNo(),
                        "opinion", opinion == null ? "" : opinion);
                if (user.phone() != null) {
                    messageClient.send(new com.examflow.registration.client.MessageServiceClient.SendReq(
                            pass ? "reg_approved" : "reg_rejected", user.phone(), "sms", params));
                }
                messageClient.send(new com.examflow.registration.client.MessageServiceClient.SendReq(
                        pass ? "reg_approved" : "reg_rejected", String.valueOf(user.userId()), "site", params));
            }
        } catch (Exception e) {
            log.warn("审核通知发送失败(不影响主流程): reg={}", reg.getId(), e);
        }
    }

    /** 排考:分配场次与机位,冲突检测(FR-SCHED-05)。 */
    @Transactional
    public void assignSlot(Long registrationId, Long slotId) {
        ExamRegistration reg = requireRegistration(registrationId);
        ExamSlot slot = requireSlot(slotId);
        if (!"approved".equals(reg.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅已通过审核的报名可排考");
        }
        if (slot.getSeatCount() >= slot.getCapacity()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "场次机位已满");
        }
        // 冲突检测:同一考生同时段不得参加其他场次(跨考次同样检测)
        List<ExamRegistration> others = registrationMapper.selectList(Wrappers.lambdaQuery(ExamRegistration.class)
                .eq(ExamRegistration::getUserId, reg.getUserId())
                .eq(ExamRegistration::getStatus, "approved")
                .isNotNull(ExamRegistration::getSlotId)
                .ne(ExamRegistration::getId, registrationId));
        for (ExamRegistration other : others) {
            ExamSlot otherSlot = slotMapper.selectById(other.getSlotId());
            if (otherSlot != null && overlap(slot, otherSlot)) {
                throw new BusinessException(ErrorCode.BIZ_ERROR,
                        "与场次[" + otherSlot.getSlotName() + "]时间冲突,请选择其他场次");
            }
        }
        reg.setSlotId(slotId);
        reg.setSeatNo(genSeatNo(slotId, slot.getSeatCount() + 1));
        registrationMapper.updateById(reg);
        slot.setSeatCount(slot.getSeatCount() + 1);
        slotMapper.updateById(slot);
    }

    // ---------- 场次(FR-SCHED-01/02) ----------

    public List<ExamSlot> slots(Long planId) {
        return slotMapper.selectList(Wrappers.lambdaQuery(ExamSlot.class)
                .eq(ExamSlot::getPlanId, planId).orderByAsc(ExamSlot::getStartTime));
    }

    @Transactional
    public Long createSlot(SlotReq req) {
        ExamPlan plan = requirePlan(req.planId());
        if (!"approved".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅已审批计划可建场次");
        }
        ExamSlot slot = new ExamSlot();
        slot.setPlanId(req.planId());
        slot.setSlotName(req.slotName());
        slot.setStartTime(req.startTime());
        slot.setEndTime(req.endTime());
        slot.setCapacity(req.capacity());
        slot.setSeatCount(0);
        slotMapper.insert(slot);
        return slot.getId();
    }

    /** 当前用户的报名列表(含计划信息,考生门户"我的考试")。 */
    public List<Map<String, Object>> myRegistrations(Long userId) {
        List<ExamRegistration> regs = registrationMapper.selectList(Wrappers.lambdaQuery(ExamRegistration.class)
                .eq(ExamRegistration::getUserId, userId).orderByDesc(ExamRegistration::getId));
        return regs.stream().map(reg -> {
            ExamPlan plan = planMapper.selectById(reg.getPlanId());
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("registrationId", reg.getId());
            row.put("planId", reg.getPlanId());
            row.put("planName", plan == null ? null : plan.getName());
            row.put("examDate", plan == null ? null : plan.getExamDate());
            row.put("status", reg.getStatus());
            row.put("ticketNo", reg.getTicketNo());
            row.put("seatNo", reg.getSeatNo());
            row.put("createTime", reg.getCreateTime());
            return row;
        }).toList();
    }

    // ---------- 名单与导出(FR-REG-04) ----------

    public PageResult<Map<String, Object>> registrations(long page, long size, Long planId, String status) {
        var wrapper = Wrappers.lambdaQuery(ExamRegistration.class)
                .eq(ExamRegistration::getPlanId, planId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ExamRegistration::getStatus, status);
        }
        wrapper.orderByAsc(ExamRegistration::getId);
        Page<ExamRegistration> p = registrationMapper.selectPage(new Page<>(page, size), wrapper);
        // 批量拉用户信息
        List<Long> userIds = p.getRecords().stream().map(ExamRegistration::getUserId).distinct().toList();
        final Map<Long, UserInfo> users;
        if (!userIds.isEmpty()) {
            users = userIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> userClient.getUser(id)));
        } else {
            users = Map.of();
        }
        return PageResult.of(p.convert(reg -> {
            UserInfo u = users.get(reg.getUserId());
            // 注意:Map.of 不允许 null 值,故用 HashMap
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("id", reg.getId());
            row.put("userId", reg.getUserId());
            row.put("username", u == null ? null : u.username());
            row.put("name", u == null ? null : u.name());
            row.put("orgId", u == null ? null : u.orgId());
            row.put("slotId", reg.getSlotId());
            row.put("seatNo", reg.getSeatNo());
            row.put("status", reg.getStatus());
            row.put("ticketNo", reg.getTicketNo());
            row.put("createTime", reg.getCreateTime());
            return row;
        }));
    }

    /** 报名名单导出(EasyExcel)。 */
    public void exportRegistrations(java.io.OutputStream out, Long planId) {
        ExamPlan plan = requirePlan(planId);
        List<ExamRegistration> regs = registrationMapper.selectList(Wrappers.lambdaQuery(ExamRegistration.class)
                .eq(ExamRegistration::getPlanId, planId).orderByAsc(ExamRegistration::getId));
        List<Map<String, Object>> rows = regs.stream().map(reg -> {
            UserInfo u = userClient.getUser(reg.getUserId());
            return Map.<String, Object>of(
                    "考试名称", plan.getName(),
                    "账号", u == null ? "" : u.username(),
                    "姓名", u == null ? "" : u.name(),
                    "状态", reg.getStatus(),
                    "准考证号", reg.getTicketNo() == null ? "" : reg.getTicketNo(),
                    "场次", reg.getSlotId() == null ? "" : String.valueOf(reg.getSlotId()),
                    "机位号", reg.getSeatNo() == null ? "" : reg.getSeatNo());
        }).toList();
        EasyExcel.write(out).sheet("报名名单").doWrite(rows);
    }

    // ---------- 辅助 ----------

    /** 条件规则自动预审:规则为空 → 通过;命中 orgIds/userTypes → 通过,否则转人工。 */
    private String autoAudit(ExamPlan plan, UserInfo user) {
        if (plan.getConditionRule() == null || plan.getConditionRule().isBlank()) {
            return "approved";
        }
        try {
            JsonNode rule = objectMapper.readTree(plan.getConditionRule());
            boolean orgOk = true;
            if (rule.has("orgIds") && rule.path("orgIds").isArray() && !rule.path("orgIds").isEmpty()) {
                orgOk = false;
                for (JsonNode org : rule.path("orgIds")) {
                    if (org.asLong() == (user.orgId() == null ? -1 : user.orgId())) {
                        orgOk = true;
                        break;
                    }
                }
            }
            boolean typeOk = true;
            if (rule.has("userTypes") && rule.path("userTypes").isArray() && !rule.path("userTypes").isEmpty()) {
                typeOk = false;
                for (JsonNode t : rule.path("userTypes")) {
                    if (t.asText().equals(user.userType())) {
                        typeOk = true;
                        break;
                    }
                }
            }
            return orgOk && typeOk ? "approved" : "pending";
        } catch (Exception e) {
            return "pending";
        }
    }

    private boolean overlap(ExamSlot a, ExamSlot b) {
        return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }

    private String genTicketNo(Long planId, Long registrationId) {
        return "TK-" + planId + "-" + String.format("%06d", registrationId);
    }

    private String genSeatNo(Long slotId, int seq) {
        return "S" + slotId + "-" + String.format("%03d", seq);
    }

    private ExamPlan requirePlan(Long id) {
        ExamPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "考试计划不存在");
        }
        return plan;
    }

    private ExamRegistration requireRegistration(Long id) {
        ExamRegistration reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        return reg;
    }

    private ExamSlot requireSlot(Long id) {
        ExamSlot slot = slotMapper.selectById(id);
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场次不存在");
        }
        return slot;
    }

    public record PlanReq(String name, Long subjectId, Long paperId,
                          LocalDateTime regStart, LocalDateTime regEnd,
                          LocalDateTime examDate, Integer capacity, String conditionRule) {
    }

    public record SlotReq(Long planId, String slotName, LocalDateTime startTime,
                          LocalDateTime endTime, Integer capacity) {
    }
}
