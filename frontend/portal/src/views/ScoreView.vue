<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '../api/request'

/**
 * 成绩查询:我的成绩(公示/已发布)+ 公示期申诉。
 */
const scores = ref<any[]>([])
const appealInput = ref('')
const appealTarget = ref<number | null>(null)
const message = ref('')

async function load() {
  try {
    scores.value = await request.get('/grading/scores/my')
  } catch {
    scores.value = []
  }
}

async function submitAppeal() {
  if (!appealTarget.value || !appealInput.value.trim()) return
  try {
    await request.post(`/grading/scores/${appealTarget.value}/appeal`, { reason: appealInput.value.trim() })
    message.value = '申诉已提交,复核结果将另行通知'
    appealInput.value = ''
    appealTarget.value = null
  } catch (e) {
    message.value = (e as Error).message
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="ef-section-title">成绩查询</h1>

    <table class="ef-table">
      <thead>
        <tr>
          <th class="num">总分</th>
          <th class="num">客观题</th>
          <th class="num">主观题</th>
          <th>是否合格</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in scores" :key="s.sessionId">
          <td class="num">{{ s.totalScore }}</td>
          <td class="num">{{ s.objectiveScore }}</td>
          <td class="num">{{ s.subjectiveScore }}</td>
          <td>
            <span class="ef-status" :class="s.passFlag === 1 ? 'is-success' : 'is-error'">
              {{ s.passFlag === 1 ? '合格' : '不合格' }}
            </span>
          </td>
          <td>
            <span class="ef-status" :class="s.publishStatus === 'published' ? 'is-success' : 'is-warning'">
              {{ s.publishStatus === 'published' ? '已发布' : '公示中' }}
            </span>
          </td>
          <td>
            <button v-if="s.publishStatus === 'publicity'" class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px"
                    @click="appealTarget = s.sessionId">
              申诉
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-if="scores.length === 0" style="color: var(--ef-ink-muted); margin-top: 16px">暂无已公布的成绩。</p>

    <div v-if="appealTarget" class="ef-card" style="margin-top: 24px; max-width: 640px">
      <h2 style="font-family: var(--ef-font-heiti); font-size: 16px; margin-bottom: 12px">成绩异议申诉</h2>
      <label class="ef-form-label">申诉理由</label>
      <textarea v-model="appealInput" class="ef-input" rows="3" placeholder="请说明异议内容"></textarea>
      <div style="margin-top: 12px; text-align: right">
        <button class="ef-btn ef-btn-plain" @click="appealTarget = null">取消</button>
        <button class="ef-btn ef-btn-primary" style="margin-left: 8px" @click="submitAppeal">提交申诉</button>
      </div>
    </div>

    <p v-if="message" style="color: var(--ef-primary); margin-top: 16px">{{ message }}</p>

    <p style="color: var(--ef-ink-muted); margin-top: 24px">
      成绩公布后设 3 个工作日公示期,公示期内可提出成绩异议申诉。
    </p>
  </div>
</template>
