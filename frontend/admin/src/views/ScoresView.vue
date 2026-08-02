<script setup lang="ts">
import { ref } from 'vue'
import request from '../api/request'

/**
 * 成绩管理:成绩发布(进入公示期)、成绩导出。
 */
const planId = ref(1)
const publicityDays = ref(3)
const message = ref('')

async function publish() {
  message.value = ''
  try {
    const count = await request.post('/grading/scores/publish', { planId: planId.value, publicityDays: publicityDays.value })
    message.value = `发布成功,${count} 名考生进入 ${publicityDays.value} 天公示期(已发站内信通知)`
  } catch (e) {
    message.value = (e as Error).message
  }
}

function exportScores() {
  const token = localStorage.getItem('examflow_token')
  const url = `/api/v1/grading/scores/export?planId=${planId.value}`
  // 用 fetch 带令牌下载
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .then((r) => r.blob())
    .then((blob) => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = `scores-plan${planId.value}.xlsx`
      a.click()
    })
}
</script>

<template>
  <div>
    <h1 class="ef-page-title">成绩管理</h1>

    <div class="ef-card">
      <h2 style="font-family: var(--ef-font-heiti); font-size: 16px; margin-bottom: 12px">成绩发布</h2>
      <div style="display: flex; gap: 8px; align-items: center">
        <label class="ef-form-label" style="margin: 0">考次 ID</label>
        <input v-model.number="planId" type="number" class="ef-input" style="width: 100px" />
        <label class="ef-form-label" style="margin: 0">公示天数</label>
        <input v-model.number="publicityDays" type="number" class="ef-input" style="width: 80px" />
        <button class="ef-btn ef-btn-primary" @click="publish">发布成绩</button>
        <button class="ef-btn ef-btn-outline" @click="exportScores">导出成绩</button>
      </div>
      <p v-if="message" style="color: var(--ef-primary); margin-top: 8px; font-size: 13px">{{ message }}</p>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>流程</th>
            <th>说明</th>
          </tr>
        </thead>
        <tbody>
          <tr><td>评阅</td><td>客观题自动判分,主观题双评/仲裁,完成后自动结算总分与及格判定</td></tr>
          <tr><td>发布</td><td>进入公示期(默认 3 个工作日),考生可见成绩并可申诉</td></tr>
          <tr><td>更正</td><td>发布后修正须走申请→审批流程,全程留痕</td></tr>
          <tr><td>导出</td><td>按考次导出成绩单(含姓名/分项/合格判定)</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
