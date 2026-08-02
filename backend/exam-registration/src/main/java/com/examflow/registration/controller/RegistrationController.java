package com.examflow.registration.controller;

import com.examflow.common.audit.AuditLog;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.core.Result;
import com.examflow.registration.entity.ExamPlan;
import com.examflow.registration.entity.ExamSlot;
import com.examflow.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报名排考接口(FR-REG/FR-SCHED)。
 * 考生报名走网关 X-User-Id(当前用户);管理操作为后续方法级授权预留。
 */
@RestController
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
@Tag(name = "报名排考服务")
public class RegistrationController {

    private final RegistrationService registrationService;

    // ---------- 考试计划 ----------

    @GetMapping("/plans")
    @Operation(summary = "考试计划列表(考生可按状态查看)")
    public Result<PageResult<ExamPlan>> plans(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "20") long size,
                                              @RequestParam(required = false) String status) {
        return Result.ok(registrationService.plans(page, size, status));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "考试计划详情")
    public Result<ExamPlan> planDetail(@PathVariable Long id) {
        return Result.ok(registrationService.planDetail(id));
    }

    @PostMapping("/plans")
    @Operation(summary = "创建考试计划(含报名条件规则)")
    @AuditLog(module = "registration", action = "创建考试计划")
    public Result<Long> createPlan(@RequestBody RegistrationService.PlanReq req) {
        return Result.ok(registrationService.createPlan(req));
    }

    @PostMapping("/plans/{id}/submit")
    @Operation(summary = "计划送审")
    @AuditLog(module = "registration", action = "考试计划送审")
    public Result<Void> submitPlan(@PathVariable Long id) {
        registrationService.submitPlan(id);
        return Result.ok();
    }

    @PostMapping("/plans/{id}/audit")
    @Operation(summary = "计划审批")
    @AuditLog(module = "registration", action = "考试计划审批")
    public Result<Void> auditPlan(@PathVariable Long id, @RequestParam boolean pass,
                                  @RequestParam(required = false) String opinion) {
        registrationService.auditPlan(id, pass, opinion);
        return Result.ok();
    }

    // ---------- 报名 ----------

    @PostMapping("/apply")
    @Operation(summary = "考生报名(自动预审,命中规则直接发放准考证)")
    @AuditLog(module = "registration", action = "提交报名")
    public Result<Long> apply(@RequestBody ApplyReq req) {
        Long userId = com.examflow.common.context.UserContext.requireUserId();
        return Result.ok(registrationService.apply(userId, req.planId(), req.slotId()));
    }

    @PostMapping("/apply/{id}/audit")
    @Operation(summary = "报名人工审核(通过发放准考证/驳回释放名额)")
    @AuditLog(module = "registration", action = "报名审核")
    public Result<Void> auditRegistration(@PathVariable Long id, @RequestParam boolean pass,
                                          @RequestParam(required = false) String opinion) {
        registrationService.auditRegistration(id, pass, opinion);
        return Result.ok();
    }

    @GetMapping("/my")
    @Operation(summary = "我的报名列表(考生门户)")
    public Result<List<Map<String, Object>>> myRegistrations() {
        return Result.ok(registrationService.myRegistrations(
                com.examflow.common.context.UserContext.requireUserId()));
    }

    @GetMapping("/plans/{planId}/registrations")
    @Operation(summary = "报名名单(分页)")
    public Result<PageResult<Map<String, Object>>> registrations(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status) {
        return Result.ok(registrationService.registrations(page, size, planId, status));
    }

    @GetMapping("/plans/{planId}/registrations/export")
    @Operation(summary = "报名名单导出 Excel")
    public void export(@PathVariable Long planId, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=registrations.xlsx");
        registrationService.exportRegistrations(response.getOutputStream(), planId);
    }

    // ---------- 场次与排考 ----------

    @GetMapping("/plans/{planId}/slots")
    @Operation(summary = "场次列表")
    public Result<List<ExamSlot>> slots(@PathVariable Long planId) {
        return Result.ok(registrationService.slots(planId));
    }

    @PostMapping("/slots")
    @Operation(summary = "创建场次")
    @AuditLog(module = "registration", action = "创建场次")
    public Result<Long> createSlot(@RequestBody RegistrationService.SlotReq req) {
        return Result.ok(registrationService.createSlot(req));
    }

    @PostMapping("/registrations/{id}/assign-slot")
    @Operation(summary = "排考:分配场次机位(冲突检测)")
    @AuditLog(module = "registration", action = "排考分配")
    public Result<Void> assignSlot(@PathVariable Long id, @RequestParam Long slotId) {
        registrationService.assignSlot(id, slotId);
        return Result.ok();
    }

    public record ApplyReq(Long planId, Long slotId) {
    }
}
