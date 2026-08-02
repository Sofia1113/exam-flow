<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

/**
 * 考试房间:对接考试服务真实接口(FR-EXAM / TDD §3)。
 * - 进入:POST /exam/sessions(registrationId 从 URL 携带)
 * - 作答:本地草稿 + 30 秒自动保存(seq 递增增量)+ 15 秒心跳
 * - 交卷:POST submit(幂等),交卷后锁定
 * - 断线:进入时提示恢复(resume 由后端会话状态驱动)
 */
interface Question {
  seq: number
  type: string
  stem: string
  options: string | null
  score: number
}

const route = useRoute()
const registrationId = Number(route.query.registrationId)

// 令牌:门户跳转时经 URL 传递(跨端口 localStorage 不共享);
// 生产建议与门户同域部署或走 OAuth 跳转,避免令牌入 URL
const urlToken = route.query.token as string | undefined
if (urlToken) {
  localStorage.setItem('examflow_token', urlToken)
}

const api = axios.create({ baseURL: '/api/v1', timeout: 15_000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('examflow_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const stage = ref<'entering' | 'notice' | 'answering' | 'submitted' | 'error'>('entering')
const errorMsg = ref('')
const sessionId = ref(0)
const questions = ref<Question[]>([])
const deadlineAt = ref(0)
const remainSec = ref(0)
const current = ref(1)
const marked = reactive<Record<number, boolean>>({})

/** 作答:卷面序号 → 答案字符串 */
const answers = reactive<Record<number, string>>({})

/** 本地增量序号(服务端 seq 对齐) */
let seqCounter = 0
let lastSeq = 0
let saveTimer: number | undefined
let heartbeatTimer: number | undefined
let countdownTimer: number | undefined

const remainText = computed(() => {
  const h = Math.floor(remainSec.value / 3600)
  const m = Math.floor((remainSec.value % 3600) / 60)
  const s = remainSec.value % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const answeredCount = computed(() => Object.keys(answers).filter((k) => (answers[Number(k)] || '').trim() !== '').length)

function parseOptions(q: Question): { key: string; text: string }[] {
  if (!q.options) return []
  try {
    return JSON.parse(q.options)
  } catch {
    return []
  }
}

function selectOption(key: string) {
  const q = questions.value.find((x) => x.seq === current.value)
  if (!q) return
  if (q.type === 'multiple') {
    const list = (answers[current.value] || '').split(',').filter(Boolean)
    const idx = list.indexOf(key)
    if (idx >= 0) list.splice(idx, 1)
    else list.push(key)
    answers[current.value] = list.join(',')
  } else {
    answers[current.value] = key
  }
  saveDraft()
}

function isSelected(key: string): boolean {
  return (answers[current.value] || '').split(',').includes(key)
}

function saveDraft() {
  localStorage.setItem('exam-draft', JSON.stringify({ sessionId: sessionId.value, answers, marked }))
}

function loadDraft() {
  try {
    const raw = localStorage.getItem('exam-draft')
    if (!raw) return
    const draft = JSON.parse(raw)
    if (draft.sessionId === sessionId.value) {
      Object.assign(answers, draft.answers || {})
      Object.assign(marked, draft.marked || {})
    }
  } catch {
    /* 草稿损坏忽略 */
  }
}

async function enter() {
  stage.value = 'entering'
  try {
    const data = await api.post('/exam/sessions', { registrationId, deviceFp: 'browser-' + Date.now() })
    sessionId.value = data.data.sessionId
    questions.value = data.data.questions
    deadlineAt.value = Date.parse(data.data.deadlineAt)
    remainSec.value = Math.max(0, Math.floor((deadlineAt.value - Date.now()) / 1000))
    loadDraft()
    stage.value = 'notice'
  } catch (e: any) {
    // 已存在会话 → 尝试恢复
    const msg = e?.response?.data?.message || e.message
    if (msg.includes('已进入考试')) {
      stage.value = 'error'
      errorMsg.value = '检测到进行中的考试会话,请点击"恢复考试"继续作答'
    } else {
      stage.value = 'error'
      errorMsg.value = msg
    }
  }
}

async function resume() {
  try {
    const data = await api.post(`/exam/sessions/${sessionId.value}/resume`, { registrationId })
    questions.value = data.data.questions
    deadlineAt.value = Date.parse(data.data.deadlineAt)
    remainSec.value = data.data.remainSeconds
    lastSeq = data.data.lastSeq
    seqCounter = lastSeq
    data.data.answers.forEach((a: any) => (answers[a.questionSeq] = a.answer))
    startAnswering()
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message || '恢复失败'
  }
}

function startAnswering() {
  stage.value = 'answering'
  // 30 秒自动保存 + 15 秒心跳 + 倒计时
  saveTimer = window.setInterval(saveAnswers, 30_000)
  heartbeatTimer = window.setInterval(() => {
    api.post(`/exam/sessions/${sessionId.value}/heartbeat`, { registrationId }).catch(() => {})
  }, 15_000)
  countdownTimer = window.setInterval(() => {
    remainSec.value = Math.max(0, Math.floor((deadlineAt.value - Date.now()) / 1000))
    if (remainSec.value <= 0) submit()
  }, 1000)
}

/** 保存增量:仅发送自上次以来的变更(本地队列模拟) */
async function saveAnswers() {
  const items: any[] = []
  for (const [seqStr, answer] of Object.entries(answers)) {
    const seq = Number(seqStr)
    if (seq > lastSeq) {
      items.push({ seq: ++seqCounter, questionSeq: seq, answer })
    }
  }
  if (!items.length) return
  try {
    const data = await api.post(`/exam/sessions/${sessionId.value}/answers`, {
      registrationId,
      fromSeq: lastSeq,
      answers: items
    })
    lastSeq = data.data
    seqCounter = Math.max(seqCounter, lastSeq)
  } catch {
    /* 网络异常:本地草稿保留,下轮重发 */
  }
}

async function submit() {
  if (!window.confirm(`尚有 ${questions.value.length - answeredCount.value} 题未作答,确认交卷?`)) return
  const items: any[] = []
  for (const [seqStr, answer] of Object.entries(answers)) {
    const seq = Number(seqStr)
    if (seq > lastSeq) {
      items.push({ seq: ++seqCounter, questionSeq: seq, answer })
    }
  }
  try {
    await api.post(`/exam/sessions/${sessionId.value}/submit`, { registrationId, sign: 'client', answers: items })
    stage.value = 'submitted'
    localStorage.removeItem('exam-draft')
    if (saveTimer) clearInterval(saveTimer)
    if (heartbeatTimer) clearInterval(heartbeatTimer)
    if (countdownTimer) clearInterval(countdownTimer)
  } catch (e: any) {
    alert('交卷失败:' + (e?.response?.data?.message || '请重试'))
  }
}

function toggleMark() {
  marked[current.value] = !marked[current.value]
  saveDraft()
}

onMounted(enter)
onBeforeUnmount(() => {
  if (saveTimer) clearInterval(saveTimer)
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  saveDraft()
})
</script>

<template>
  <!-- 进入中 -->
  <div v-if="stage === 'entering'" class="ef-center-screen">
    <div class="ef-center-card" style="text-align: center">
      <h1>正在进入考试...</h1>
      <p>正在校验考试资格并抽取试卷</p>
    </div>
  </div>

  <!-- 错误/需恢复 -->
  <div v-else-if="stage === 'error'" class="ef-center-screen">
    <div class="ef-center-card">
      <h1>无法进入考试</h1>
      <p>{{ errorMsg }}</p>
      <div style="margin-top: 24px; text-align: center">
        <button v-if="errorMsg.includes('恢复')" class="ef-btn ef-btn-primary" @click="resume">恢复考试</button>
        <button v-else class="ef-btn" @click="enter">重试</button>
      </div>
    </div>
  </div>

  <!-- 考前须知 -->
  <div v-else-if="stage === 'notice'" class="ef-center-screen">
    <div class="ef-center-card">
      <h1>考试须知</h1>
      <p>一、本场考试共 {{ questions.length }} 题,答题时间以页面倒计时为准。</p>
      <p>二、作答将每 30 秒自动保存;断线 5 分钟内不丢失,可重新进入恢复。</p>
      <p>三、考试全程开启行为监控,切屏累计超过阈值将按违规处理。</p>
      <p>四、交卷后不可再次进入考试,请确认作答完毕。</p>
      <div style="margin-top: 24px; text-align: center">
        <button class="ef-btn ef-btn-primary" @click="startAnswering">我已阅读并同意,开始考试</button>
      </div>
    </div>
  </div>

  <!-- 交卷完成 -->
  <div v-else-if="stage === 'submitted'" class="ef-center-screen">
    <div class="ef-center-card" style="text-align: center">
      <h1>交卷成功</h1>
      <p>您的答卷已提交,请关闭本窗口。成绩公布后可在门户查询。</p>
    </div>
  </div>

  <!-- 作答界面 -->
  <div v-else>
    <header class="ef-exam-topbar">
      <span class="ef-exam-title">在线考试</span>
      <span class="ef-countdown" :class="{ 'is-warning': remainSec < 300 }">剩余时间 {{ remainText }}</span>
    </header>

    <div class="ef-exam-body">
      <aside class="ef-answer-sheet">
        <h3>答题卡(已答 {{ answeredCount }}/{{ questions.length }})</h3>
        <div class="ef-sheet-grid">
          <button v-for="q in questions" :key="q.seq" class="ef-sheet-item"
                  :class="{ 'is-current': current === q.seq, 'is-answered': (answers[q.seq] || '').trim() !== '', 'is-marked': marked[q.seq] }"
                  @click="current = q.seq">
            {{ q.seq }}
          </button>
        </div>
      </aside>

      <main class="ef-question-area">
        <div v-for="q in questions" v-show="current === q.seq" :key="q.seq" class="ef-question">
          <div class="ef-question-header">
            <span class="ef-q-no">第 {{ q.seq }} 题 · {{ q.type }}</span>
            <span class="ef-q-score">本题 {{ q.score }} 分</span>
          </div>

          <p class="ef-question-stem">{{ q.stem }}</p>

          <!-- 选择/判断题 -->
          <template v-if="q.type === 'single' || q.type === 'judge'">
            <label v-for="opt in parseOptions(q)" :key="opt.key" class="ef-option"
                   :class="{ 'is-selected': answers[q.seq] === opt.key }">
              <input type="radio" :name="'q' + q.seq" :checked="answers[q.seq] === opt.key"
                     @change="answers[q.seq] = opt.key; saveDraft()" style="display: none" />
              <span class="ef-option-key">{{ opt.key }}</span>
              <span>{{ opt.text }}</span>
            </label>
            <div v-if="q.type === 'judge' && !q.options" class="ef-option" style="cursor: default">
              <span class="ef-option-key">{{ answers[q.seq] === 'true' ? '✓ 正确' : '✗ 错误' }}</span>
            </div>
          </template>

          <!-- 多选题 -->
          <template v-else-if="q.type === 'multiple'">
            <label v-for="opt in parseOptions(q)" :key="opt.key" class="ef-option"
                   :class="{ 'is-selected': isSelected(opt.key) }">
              <input type="checkbox" :checked="isSelected(opt.key)" @change="selectOption(opt.key)" style="display: none" />
              <span class="ef-option-key">{{ opt.key }}</span>
              <span>{{ opt.text }}</span>
            </label>
          </template>

          <!-- 填空题 -->
          <template v-else-if="q.type === 'fill'">
            <textarea class="ef-input" :value="answers[q.seq] || ''" rows="3"
                      placeholder="多个空用逗号分隔" @input="answers[q.seq] = ($event.target as HTMLTextAreaElement).value; saveDraft()"></textarea>
          </template>

          <!-- 主观/案例/操作题 -->
          <template v-else>
            <textarea class="ef-input" :value="answers[q.seq] || ''" rows="8"
                      placeholder="请输入作答内容" @input="answers[q.seq] = ($event.target as HTMLTextAreaElement).value; saveDraft()"></textarea>
          </template>

          <div class="ef-action-bar">
            <button class="ef-btn" @click="toggleMark">{{ marked[q.seq] ? '取消标记' : '标记本题' }}</button>
            <div>
              <button class="ef-btn" :disabled="current === 1" @click="current--" style="margin-right: 12px">上一题</button>
              <button v-if="current < questions.length" class="ef-btn" @click="current++">下一题</button>
              <button v-else class="ef-btn ef-btn-primary" @click="submit">交卷</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
