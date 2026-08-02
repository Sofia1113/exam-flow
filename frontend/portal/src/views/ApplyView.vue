<script setup lang="ts">
import { reactive, ref } from 'vue'

/**
 * 考试报名(骨架):报名条件、资格预审由 registration-service 处理。
 */
const plans = [
  { id: 1, name: '2026 年度职称认定考试', subject: '综合能力', regWindow: '2026-08-01 ~ 2026-08-31', status: '报名中' },
  { id: 2, name: '2026 年下半年岗位能力测评', subject: '岗位技能', regWindow: '2026-09-01 ~ 2026-09-20', status: '未开始' }
]

const form = reactive({
  planId: 0,
  name: '',
  idCard: '',
  org: ''
})

const submitted = ref(false)

function submit() {
  // TODO: 调用 POST /api/v1/registration/apply
  submitted.value = true
}
</script>

<template>
  <div>
    <h1 class="ef-section-title">考试报名</h1>

    <table class="ef-table">
      <thead>
        <tr>
          <th>考试名称</th>
          <th>科目</th>
          <th>报名时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="plan in plans" :key="plan.id">
          <td>{{ plan.name }}</td>
          <td>{{ plan.subject }}</td>
          <td>{{ plan.regWindow }}</td>
          <td><span class="ef-status" :class="plan.status === '报名中' ? 'is-success' : ''">{{ plan.status }}</span></td>
          <td><button class="ef-btn ef-btn-primary" style="padding: 6px 16px; font-size: 14px">报名</button></td>
        </tr>
      </tbody>
    </table>

    <h2 class="ef-section-title">报名信息填报</h2>
    <div class="ef-card" style="max-width: 640px">
      <form v-if="!submitted" @submit.prevent="submit">
        <div style="margin-bottom: 16px">
          <label class="ef-form-label required">报考考试</label>
          <select v-model="form.planId" class="ef-input">
            <option :value="0" disabled>请选择考试</option>
            <option v-for="plan in plans" :key="plan.id" :value="plan.id">{{ plan.name }}</option>
          </select>
        </div>
        <div style="margin-bottom: 16px">
          <label class="ef-form-label required">姓名</label>
          <input v-model="form.name" class="ef-input" placeholder="请输入真实姓名" />
        </div>
        <div style="margin-bottom: 16px">
          <label class="ef-form-label required">身份证号</label>
          <input v-model="form.idCard" class="ef-input" placeholder="身份证号格式不正确时无法提交" />
        </div>
        <div style="margin-bottom: 24px">
          <label class="ef-form-label">工作单位</label>
          <input v-model="form.org" class="ef-input" placeholder="请输入工作单位" />
        </div>
        <button type="submit" class="ef-btn ef-btn-primary">提交报名</button>
      </form>
      <div v-else class="ef-status is-success">报名信息已提交,等待资格审核。</div>
    </div>
  </div>
</template>
