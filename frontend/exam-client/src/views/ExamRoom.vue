<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

/**
 * 考试房间(骨架,PRD FR-EXAM / TDD §3)。
 *
 * 生产化 TODO(核心链路):
 * 1. 进入:POST /api/v1/exam/sessions 校验 + 抽卷,进入全屏;
 * 2. 本地草稿:作答实时暂存 localStorage,每 30 秒自动保存(POST answers,seq 自增),
 *    断网时本地缓存继续作答,恢复后按 lastSeq 重发(TDD §3.2);
 * 3. 倒计时以服务端 deadlineAt 为准(服务器时间),禁止客户端改时间;
 * 4. 交卷:POST submit 携带增量序列 hash 签名,幂等重试;
 * 5. 防作弊:全屏锁定、切屏/离屏事件上报(POST behaviors)、水印叠加。
 */

/** 阶段:notice(考前须知)→ answering → submitted */
const stage = ref<'notice' | 'answering' | 'submitted'>('notice')

const questionCount = 30
const durationMin = 120

/** 服务端下发(骨架占位):答题起止时间 */
const deadlineAt = ref(Date.now() + durationMin * 60_000)

/** 作答记录:题目序号 → 答案 */
const answers = reactive<Record<number, string>>({})
/** 标记题目 */
const marked = reactive<Record<number, boolean>>({})
/** 当前题目序号 */
const current = ref(1)

/** 倒计时(基于服务端 deadlineAt) */
const remainSec = ref(durationMin * 60)
const remainText = computed(() => {
  const h = Math.floor(remainSec.value / 3600)
  const m = Math.floor((remainSec.value % 3600) / 60)
  const s = remainSec.value % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(h)}:${pad(m)}:${pad(s)}`
})

let timer: number | undefined

/** 本地草稿:localStorage 即时暂存(断网可续) */
function saveDraft() {
  localStorage.setItem('exam-draft', JSON.stringify({ answers, marked, current: current.value }))
}

function loadDraft() {
  const raw = localStorage.getItem('exam-draft')
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    Object.assign(answers, draft.answers ?? {})
    Object.assign(marked, draft.marked ?? {})
    current.value = draft.current ?? 1
  } catch {
    /* 草稿损坏则忽略 */
  }
}

function start() {
  stage.value = 'answering'
  // TODO: 全屏锁定 + 事件上报注册
  timer = window.setInterval(() => {
    remainSec.value = Math.max(0, Math.floor((deadlineAt.value - Date.now()) / 1000))
    if (remainSec.value <= 0) submit()
  }, 1000)
}

function goTo(index: number) {
  current.value = index
  saveDraft()
}

function toggleMark() {
  marked[current.value] = !marked[current.value]
  saveDraft()
}

function submit() {
  if (!window.confirm(`剩余未作答 ${unanswered.value} 题,确认交卷?`)) return
  stage.value = 'submitted'
  // TODO: POST /api/v1/exam/sessions/{id}/submit(增量 + 签名,幂等)
  localStorage.removeItem('exam-draft')
}

const answered = computed(() => Object.keys(answers).length)
const unanswered = computed(() => questionCount - Object.keys(answers).length)

onMounted(() => {
  loadDraft()
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  saveDraft()
})
</script>

<template>
  <!-- 考前须知 -->
  <div v-if="stage === 'notice'" class="ef-center-screen">
    <div class="ef-center-card">
      <h1>考试须知</h1>
      <p>一、本场考试共 {{ questionCount }} 题,考试时间 {{ durationMin }} 分钟,总分以试卷发布为准。</p>
      <p>二、考试期间请保持网络畅通;断线 5 分钟内作答不丢失,可重新进入恢复。</p>
      <p>三、考试全程开启行为监控,切屏累计超过阈值将按违规处理。</p>
      <p>四、交卷后不可再次进入考试,请确认作答完毕。</p>
      <div style="margin-top: 24px; text-align: center">
        <button class="ef-btn ef-btn-primary" @click="start">我已阅读并同意,开始考试</button>
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
  <div v-else class="ef-exam-room">
    <header class="ef-exam-topbar">
      <span class="ef-exam-title">2026 年度职称认定考试 · 综合能力</span>
      <span class="ef-countdown" :class="{ 'is-warning': remainSec < 300 }">剩余时间 {{ remainText }}</span>
    </header>

    <div class="ef-exam-body">
      <aside class="ef-answer-sheet">
        <h3>答题卡(已答 {{ answered }}/{{ questionCount }})</h3>
        <div class="ef-sheet-grid">
          <button
            v-for="i in questionCount"
            :key="i"
            class="ef-sheet-item"
            :class="{
              'is-current': current === i,
              'is-answered': answers[i],
              'is-marked': marked[i] && !answers[i]
            }"
            @click="goTo(i)"
          >
            {{ i }}
          </button>
        </div>
      </aside>

      <main class="ef-question-area">
        <div class="ef-question">
          <div class="ef-question-header">
            <span class="ef-q-no">第 {{ current }} 题</span>
            <span class="ef-q-score">本题 2 分</span>
          </div>

          <p class="ef-question-stem">
            (题干占位)下列关于考试流程管理的描述,正确的是?
          </p>

          <label class="ef-option" :class="{ 'is-selected': answers[current] === 'A' }">
            <span class="ef-option-key">A</span>
            <span>选项占位:随机抽题保证同卷不同题</span>
          </label>
          <label class="ef-option" :class="{ 'is-selected': answers[current] === 'B' }">
            <span class="ef-option-key">B</span>
            <span>选项占位:试卷快照发布后不可变更</span>
          </label>

          <div class="ef-action-bar">
            <button class="ef-btn" @click="toggleMark">
              {{ marked[current] ? '取消标记' : '标记本题' }}
            </button>
            <div>
              <button class="ef-btn" :disabled="current === 1" @click="goTo(current - 1)" style="margin-right: 12px">上一题</button>
              <button v-if="current < questionCount" class="ef-btn" @click="goTo(current + 1)">下一题</button>
              <button v-else class="ef-btn ef-btn-primary" @click="submit">交卷</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
