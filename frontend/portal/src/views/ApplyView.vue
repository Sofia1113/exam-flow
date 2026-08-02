<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '../api/request'

/**
 * 考试报名:已审批计划列表 + 在线报名。
 */
const plans = ref<any[]>([])
const message = ref('')

async function load() {
  try {
    const data = await request.get('/registration/plans', { params: { status: 'approved', size: 20 } })
    plans.value = data.list
  } catch {
    plans.value = []
  }
}

async function apply(planId: number) {
  message.value = ''
  try {
    await request.post('/registration/apply', { planId })
    message.value = '报名成功,请前往「我的考试」查看审核状态'
  } catch (e) {
    message.value = (e as Error).message
  }
}

onMounted(load)
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
          <th>名额</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="plan in plans" :key="plan.id">
          <td>{{ plan.name }}</td>
          <td>{{ plan.subjectId }}</td>
          <td>{{ (plan.regStart || '').replace('T', ' ').slice(0, 16) }} ~ {{ (plan.regEnd || '').replace('T', ' ').slice(0, 16) }}</td>
          <td>{{ plan.capacity === 0 ? '不限' : plan.capacity }}</td>
          <td>
            <button class="ef-btn ef-btn-primary" style="padding: 6px 16px; font-size: 14px" @click="apply(plan.id)">
              报名
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-if="message" class="apply-message">{{ message }}</p>
    <p v-if="plans.length === 0" style="color: var(--ef-ink-muted); margin-top: 16px">当前暂无开放报名的考试。</p>

    <h2 class="ef-section-title">报名须知</h2>
    <div class="ef-card">
      <p>一、请确保个人信息真实准确,报名条件由系统自动预审,不符条件将转人工审核。</p>
      <p>二、报名审核通过后自动生成准考证号,可在「我的考试」查看。</p>
      <p>三、名额有限,报满即止;请勿重复报名。</p>
    </div>
  </div>
</template>

<style scoped>
.apply-message {
  color: var(--ef-primary);
  margin-top: 16px;
  font-size: 15px;
}
</style>
