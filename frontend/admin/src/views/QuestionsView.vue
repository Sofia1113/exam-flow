<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

/**
 * 题库管理:分页列表(含状态流转:送审/审核/发布/停用)+ 创建题目(题型结构提示)。
 */
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const filter = reactive({ type: '', status: '', subjectId: undefined as number | undefined, keyword: '' })
const subjects = ref<any[]>([])

const showCreate = ref(false)
const form = reactive({
  type: 'single', stem: '', options: '', answer: '', analysis: '',
  difficulty: 3, subjectId: 0, tags: '', knowledges: ''
})

async function load() {
  const data = await request.get('/questions', {
    params: { page: page.value, size, ...filter, keyword: filter.keyword || undefined }
  })
  list.value = data.list
  total.value = data.total
}

async function loadSubjects() {
  subjects.value = await request.get('/questions/subjects')
}

async function create() {
  await request.post('/questions', {
    ...form,
    tags: form.tags.split(',').map((s) => s.trim()).filter(Boolean),
    knowledges: form.knowledges.split(',').map((s) => s.trim()).filter(Boolean)
  })
  showCreate.value = false
  await load()
}

async function action(id: number, action: string) {
  await request.post(`/questions/${id}/${action}`)
  await load()
}

const statusLabel: Record<string, string> = {
  draft: '草稿', pending: '待审', approved: '已审', published: '已发布', disabled: '已停用'
}

onMounted(async () => {
  await Promise.all([load(), loadSubjects()])
})
</script>

<template>
  <div>
    <h1 class="ef-page-title">题库管理</h1>

    <div class="ef-card" style="display: flex; justify-content: space-between; align-items: center; padding: 16px 24px">
      <div style="display: flex; gap: 8px">
        <select v-model="filter.type" class="ef-input" style="width: 140px">
          <option value="">全部题型</option>
          <option value="single">单选题</option>
          <option value="multiple">多选题</option>
          <option value="judge">判断题</option>
          <option value="fill">填空题</option>
          <option value="subjective">简答题</option>
          <option value="case">案例分析</option>
          <option value="operation">操作题</option>
        </select>
        <select v-model="filter.status" class="ef-input" style="width: 120px">
          <option value="">全部状态</option>
          <option value="draft">草稿</option>
          <option value="pending">待审</option>
          <option value="approved">已审</option>
          <option value="published">已发布</option>
          <option value="disabled">已停用</option>
        </select>
        <input v-model="filter.keyword" class="ef-input" style="width: 200px" placeholder="题干关键词" @keyup.enter="page = 1; load()" />
      </div>
      <div>
        <button class="ef-btn ef-btn-plain" @click="page = 1; load()">查询</button>
        <button class="ef-btn ef-btn-primary" style="margin-left: 8px" @click="showCreate = true">创建题目</button>
      </div>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>题型</th>
            <th>题干</th>
            <th class="num">难度</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in list" :key="q.id">
            <td>{{ q.id }}</td>
            <td>{{ q.type }}</td>
            <td style="max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ q.stem }}</td>
            <td class="num">{{ q.difficulty }}</td>
            <td><span class="ef-status" :class="q.status === 'published' ? 'is-success' : q.status === 'pending' ? 'is-warning' : ''">{{ statusLabel[q.status] }}</span></td>
            <td>
              <template v-if="q.status === 'draft'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(q.id, 'submit')">送审</button>
              </template>
              <template v-if="q.status === 'pending'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(q.id, 'audit?pass=true')">通过</button>
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(q.id, 'audit?pass=false')">驳回</button>
              </template>
              <template v-if="q.status === 'approved'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(q.id, 'publish')">发布</button>
              </template>
              <template v-if="q.status === 'published'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(q.id, 'disable')">停用</button>
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
        <h2 style="font-family: var(--ef-font-heiti); font-size: 18px; margin-bottom: 16px">创建题目</h2>
        <div style="margin-bottom: 12px">
          <label class="ef-form-label required">题型</label>
          <select v-model="form.type" class="ef-input">
            <option value="single">单选题</option>
            <option value="multiple">多选题</option>
            <option value="judge">判断题</option>
            <option value="fill">填空题</option>
            <option value="subjective">简答题</option>
            <option value="case">案例分析</option>
            <option value="operation">操作题</option>
          </select>
        </div>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">题干</label>
          <textarea v-model="form.stem" class="ef-input" rows="3"></textarea></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label">选项(JSON,选择题)</label>
          <textarea v-model="form.options" class="ef-input" rows="2" placeholder='[{"key":"A","text":"..."}]'></textarea></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">答案</label>
          <input v-model="form.answer" class="ef-input" placeholder='单选:A | 多选:["A","B"] | 判断:true/false | 填空:[...]' /></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label">解析</label>
          <textarea v-model="form.analysis" class="ef-input" rows="2"></textarea></div>
        <div style="margin-bottom: 12px; display: flex; gap: 12px">
          <div style="flex: 1"><label class="ef-form-label">难度</label>
            <select v-model.number="form.difficulty" class="ef-input">
              <option v-for="i in 5" :key="i" :value="i">{{ i }}</option>
            </select></div>
          <div style="flex: 1"><label class="ef-form-label required">科目</label>
            <select v-model.number="form.subjectId" class="ef-input">
              <option v-for="s in subjects" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select></div>
        </div>
        <div style="margin-bottom: 12px"><label class="ef-form-label">标签(逗号分隔)</label>
          <input v-model="form.tags" class="ef-input" placeholder="标签" style="margin-bottom: 8px" /></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label">知识点(逗号分隔)</label>
          <input v-model="form.knowledges" class="ef-input" placeholder="知识点" /></div>
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
  width: 620px;
  max-height: 90vh;
  overflow-y: auto;
  border-top: 3px solid var(--ef-primary);
}
</style>
