<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '../api/request'

/**
 * 我的考试:已报名考试列表 + 准考证 + 进入考试(跳转考试端)。
 */
const exams = ref<any[]>([])
const loading = ref(true)

async function load() {
  try {
    exams.value = await request.get('/registration/my')
  } catch (e) {
    // 未登录或接口异常:提示登录
  } finally {
    loading.value = false
  }
}

/** 进入考试:跳转考试端(令牌经 postMessage 握手传递,严禁放 URL) */
function enterExam(exam: any) {
  const examClientOrigin = import.meta.env.VITE_EXAM_CLIENT_ORIGIN || 'http://localhost:5175'
  window.open(`${examClientOrigin}/exam?registrationId=${exam.registrationId}`, '_blank')
  // 考试端发来令牌请求时,经 event.source 回发(校验来源)
  const handler = (event: MessageEvent) => {
    if (event.origin !== examClientOrigin) return
    if (event.data === 'examflow:request-token') {
      const token = localStorage.getItem('examflow_token') || ''
      event.source?.postMessage({ type: 'examflow:token', token }, { targetOrigin: event.origin })
      window.removeEventListener('message', handler)
    }
  }
  window.addEventListener('message', handler)
  window.setTimeout(() => window.removeEventListener('message', handler), 60_000)
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="ef-section-title">我的考试</h1>

    <table class="ef-table">
      <thead>
        <tr>
          <th>考试名称</th>
          <th>考试日期</th>
          <th>准考证号</th>
          <th>机位号</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="exam in exams" :key="exam.registrationId">
          <td>{{ exam.planName }}</td>
          <td>{{ (exam.examDate || '').replace('T', ' ') }}</td>
          <td>{{ exam.ticketNo || '-' }}</td>
          <td>{{ exam.seatNo || '-' }}</td>
          <td><span class="ef-status" :class="exam.status === 'approved' ? 'is-success' : exam.status === 'pending' ? 'is-warning' : ''">{{ exam.status }}</span></td>
          <td>
            <button v-if="exam.status === 'approved'" class="ef-btn ef-btn-primary" style="padding: 6px 16px; font-size: 14px" @click="enterExam(exam)">
              进入考试
            </button>
            <span v-else style="color: var(--ef-ink-subtle); font-size: 13px">等待审核</span>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-if="!loading && exams.length === 0" style="color: var(--ef-ink-muted); margin-top: 16px">
      暂无报名记录,请先前往「考试报名」。
    </p>

    <p style="color: var(--ef-ink-muted); margin-top: 16px">
      考试须知:请于开考前 30 分钟至开考后 30 分钟内进入考试;迟到 30 分钟禁止入场。考试期间断线 5 分钟内可恢复继续作答。
    </p>
  </div>
</template>
