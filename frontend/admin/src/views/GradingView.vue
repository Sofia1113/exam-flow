<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

/**
 * 阅卷管理:任务分派(双评+仲裁三人)、评阅进度、我的任务评分(脱敏)。
 */
const tasks = ref<any[]>([])
const progress = ref<any>(null)
const planId = ref(1)
const assignForm = reactive({ graderIds: '' })

const scoreInput = reactive<Record<number, string>>({})
const message = ref('')

async function loadTasks() {
  tasks.value = await request.get('/grading/tasks')
}

async function loadProgress() {
  progress.value = await request.get('/grading/progress', { params: { planId: planId.value } })
}

async function assign() {
  message.value = ''
  const graderIds = assignForm.graderIds.split(',').map((s) => Number(s.trim())).filter(Boolean)
  try {
    const created = await request.post('/grading/assign', { planId: planId.value, graderIds })
    message.value = `分派成功,生成 ${created} 个任务(一评/二评/仲裁需 3 名不同阅卷员)`
    await Promise.all([loadTasks(), loadProgress()])
  } catch (e) {
    message.value = (e as Error).message
  }
}

async function score(taskId: number) {
  try {
    await request.post(`/grading/tasks/${taskId}/score`, { score: Number(scoreInput[taskId]), comment: '在线评分' })
    delete scoreInput[taskId]
    await Promise.all([loadTasks(), loadProgress()])
  } catch (e) {
    alert((e as Error).message)
  }
}

onMounted(async () => {
  await Promise.all([loadTasks(), loadProgress()])
})
</script>

<template>
  <div>
    <h1 class="ef-page-title">阅卷管理</h1>

    <div class="ef-card">
      <h2 style="font-family: var(--ef-font-heiti); font-size: 16px; margin-bottom: 12px">任务分派</h2>
      <div style="display: flex; gap: 8px; align-items: center">
        <label class="ef-form-label" style="margin: 0">考次 ID</label>
        <input v-model.number="planId" type="number" class="ef-input" style="width: 100px" />
        <label class="ef-form-label" style="margin: 0">阅卷员 ID(≥3 人,逗号分隔)</label>
        <input v-model="assignForm.graderIds" class="ef-input" style="width: 240px" placeholder="如 1,2,5" />
        <button class="ef-btn ef-btn-primary" @click="assign">分派</button>
      </div>
      <p v-if="message" style="color: var(--ef-primary); margin-top: 8px; font-size: 13px">{{ message }}</p>
    </div>

    <div class="ef-card">
      <h2 style="font-family: var(--ef-font-heiti); font-size: 16px; margin-bottom: 12px">评阅进度</h2>
      <p v-if="progress" style="color: var(--ef-ink-muted)">
        会话 {{ progress.sessions }} · 任务 {{ progress.total }} · 待评 {{ progress.pending }} ·
        评阅中 {{ progress.grading }} · 已完成 {{ progress.graded }}
      </p>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>题型</th>
            <th>题干</th>
            <th>采分点</th>
            <th>考生作答</th>
            <th class="num">满分</th>
            <th>轮次</th>
            <th>评分</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.taskId">
            <td>{{ t.type }}</td>
            <td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ t.stem }}</td>
            <td style="max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ t.keyPoints }}</td>
            <td style="max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ t.studentAnswer }}</td>
            <td class="num">{{ t.fullScore }}</td>
            <td>{{ t.round }}</td>
            <td>
              <input v-model="scoreInput[t.taskId]" type="number" class="ef-input" style="width: 70px; display: inline-block" placeholder="分" />
              <button class="ef-btn ef-btn-primary" style="padding: 6px 12px; font-size: 13px; margin-left: 4px" @click="score(t.taskId)">提交</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="tasks.length === 0" style="padding: 16px; color: var(--ef-ink-subtle); font-size: 13px">当前没有待评任务</p>
    </div>
  </div>
</template>
