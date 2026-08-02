<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

/**
 * 试卷管理:列表 + 创建(固定/策略)+ 状态流转(送审/审批/发布)。
 */
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const subjects = ref<any[]>([])

const showCreate = ref(false)
const form = reactive({
  name: '', subjectId: 0, paperType: 'fixed', passScore: 60, durationMin: 120,
  blueprint: '{"slots":[{"type":"single","count":5,"score":2}]}',
  questions: '8:2, 9:3' // 固定卷:questionId:score 逗号分隔
})

const statusLabel: Record<string, string> = {
  draft: '草稿', pending: '待审', approved: '已审', published: '已发布', archived: '已归档'
}

async function load() {
  const data = await request.get('/papers', { params: { page: page.value, size } })
  list.value = data.list
  total.value = data.total
}

async function create() {
  const payload: any = {
    name: form.name, subjectId: form.subjectId, passScore: form.passScore,
    durationMin: form.durationMin, paperType: form.paperType
  }
  if (form.paperType === 'strategy') {
    payload.blueprint = form.blueprint
  } else {
    payload.questions = form.questions.split(',').map((s) => s.trim()).filter(Boolean)
      .map((s) => {
        const [id, score] = s.split(':')
        return { questionId: Number(id), score: Number(score) }
      })
  }
  await request.post('/papers', payload)
  showCreate.value = false
  await load()
}

async function action(id: number, action: string) {
  await request.post(`/papers/${id}/${action}`)
  await load()
}

onMounted(async () => {
  await Promise.all([load(), request.get('/questions/subjects').then((d) => (subjects.value = d))])
})
</script>

<template>
  <div>
    <h1 class="ef-page-title">试卷管理</h1>

    <div class="ef-card" style="display: flex; justify-content: flex-end; padding: 16px 24px">
      <button class="ef-btn ef-btn-primary" @click="showCreate = true">创建试卷</button>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>类型</th>
            <th class="num">总分</th>
            <th class="num">及格线</th>
            <th class="num">时长(分)</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in list" :key="p.id">
            <td>{{ p.name }}</td>
            <td>{{ p.paperType === 'fixed' ? '固定卷' : '策略卷' }}</td>
            <td class="num">{{ p.totalScore }}</td>
            <td class="num">{{ p.passScore }}</td>
            <td class="num">{{ p.durationMin }}</td>
            <td><span class="ef-status" :class="p.status === 'published' ? 'is-success' : p.status === 'pending' ? 'is-warning' : ''">{{ statusLabel[p.status] }}</span></td>
            <td>
              <template v-if="p.status === 'draft'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(p.id, 'submit')">送审</button>
              </template>
              <template v-if="p.status === 'pending'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(p.id, 'audit?pass=true')">通过</button>
              </template>
              <template v-if="p.status === 'approved'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(p.id, 'publish')">发布</button>
              </template>
              <template v-if="p.status === 'published'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(p.id, 'archive')">归档</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
      <div style="padding: 12px 16px; color: var(--ef-ink-subtle); font-size: 13px">
        共 {{ total }} 条 · 第 {{ page }} 页
        <button class="ef-btn ef-btn-plain" style="padding: 4px 12px" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <button class="ef-btn ef-btn-plain" style="padding: 4px 12px" :disabled="page * size >= total" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask">
      <div class="ef-card modal">
        <h2 style="font-family: var(--ef-font-heiti); font-size: 18px; margin-bottom: 16px">创建试卷</h2>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">试卷名称</label>
          <input v-model="form.name" class="ef-input" /></div>
        <div style="margin-bottom: 12px; display: flex; gap: 12px">
          <div style="flex: 1"><label class="ef-form-label required">科目</label>
            <select v-model.number="form.subjectId" class="ef-input">
              <option v-for="s in subjects" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select></div>
          <div style="flex: 1"><label class="ef-form-label required">类型</label>
            <select v-model="form.paperType" class="ef-input">
              <option value="fixed">固定组卷</option>
              <option value="strategy">策略组卷</option>
            </select></div>
        </div>
        <div style="margin-bottom: 12px; display: flex; gap: 12px">
          <div style="flex: 1"><label class="ef-form-label">及格线</label>
            <input v-model.number="form.passScore" type="number" class="ef-input" /></div>
          <div style="flex: 1"><label class="ef-form-label">时长(分钟)</label>
            <input v-model.number="form.durationMin" type="number" class="ef-input" /></div>
        </div>
        <div v-if="form.paperType === 'fixed'" style="margin-bottom: 12px">
          <label class="ef-form-label">选题(questionId:分值,逗号分隔)</label>
          <input v-model="form.questions" class="ef-input" />
        </div>
        <div v-else style="margin-bottom: 12px">
          <label class="ef-form-label">组卷蓝图(JSON)</label>
          <textarea v-model="form.blueprint" class="ef-input" rows="4"></textarea>
        </div>
        <div style="text-align: right">
          <button class="ef-btn ef-btn-plain" @click="showCreate = false">取消</button>
          <button class="ef-btn ef-btn-primary" style="margin-left: 8px" @click="create">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal {
  width: 560px;
  border-top: 3px solid var(--ef-primary);
}
</style>
