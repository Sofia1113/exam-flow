<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

/**
 * 考试管理:计划列表 + 创建计划 + 状态流转 + 报名审核。
 */
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 10

const showCreate = ref(false)
const form = reactive({
  name: '', subjectId: 1, paperId: 0, capacity: 100,
  regStart: '', regEnd: '', conditionRule: ''
})

const statusLabel: Record<string, string> = {
  draft: '草稿', pending: '待审', approved: '已开放报名', running: '进行中', closed: '已结束'
}

async function load() {
  const data = await request.get('/registration/plans', { params: { page: page.value, size } })
  list.value = data.list
  total.value = data.total
}

async function create() {
  await request.post('/registration/plans', {
    ...form,
    regStart: form.regStart ? new Date(form.regStart).toISOString().slice(0, 19) : null,
    regEnd: form.regEnd ? new Date(form.regEnd).toISOString().slice(0, 19) : null,
    conditionRule: form.conditionRule || null
  })
  showCreate.value = false
  await load()
}

async function action(id: number, action: string) {
  await request.post(`/registration/plans/${id}/${action}`)
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="ef-page-title">考试管理</h1>

    <div class="ef-card" style="display: flex; justify-content: flex-end; padding: 16px 24px">
      <button class="ef-btn ef-btn-primary" @click="showCreate = true">创建考试计划</button>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>考试名称</th>
            <th>报名时间</th>
            <th class="num">名额</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in list" :key="p.id">
            <td>{{ p.name }}</td>
            <td>{{ (p.regStart || '').replace('T', ' ').slice(0, 16) }}</td>
            <td class="num">{{ p.capacity === 0 ? '不限' : p.capacity }}</td>
            <td><span class="ef-status" :class="p.status === 'approved' ? 'is-success' : p.status === 'pending' ? 'is-warning' : ''">{{ statusLabel[p.status] }}</span></td>
            <td>
              <template v-if="p.status === 'draft'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(p.id, 'submit')">送审</button>
              </template>
              <template v-if="p.status === 'pending'">
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(p.id, 'audit?pass=true')">通过</button>
                <button class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px" @click="action(p.id, 'audit?pass=false')">驳回</button>
              </template>
              <template v-if="p.status === 'approved'">
                <router-link :to="`/registrations?planId=${p.id}`" class="ef-btn ef-btn-plain" style="padding: 4px 8px; font-size: 12px; text-decoration: none">报名审核</router-link>
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
        <h2 style="font-family: var(--ef-font-heiti); font-size: 18px; margin-bottom: 16px">创建考试计划</h2>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">名称</label>
          <input v-model="form.name" class="ef-input" /></div>
        <div style="margin-bottom: 12px; display: flex; gap: 12px">
          <div style="flex: 1"><label class="ef-form-label">试卷 ID</label>
            <input v-model.number="form.paperId" type="number" class="ef-input" /></div>
          <div style="flex: 1"><label class="ef-form-label">名额</label>
            <input v-model.number="form.capacity" type="number" class="ef-input" /></div>
        </div>
        <div style="margin-bottom: 12px; display: flex; gap: 12px">
          <div style="flex: 1"><label class="ef-form-label required">报名开始</label>
            <input v-model="form.regStart" type="datetime-local" class="ef-input" /></div>
          <div style="flex: 1"><label class="ef-form-label required">报名截止</label>
            <input v-model="form.regEnd" type="datetime-local" class="ef-input" /></div>
        </div>
        <div style="margin-bottom: 12px">
          <label class="ef-form-label">报名条件(JSON,如 {"orgIds":[2]})</label>
          <textarea v-model="form.conditionRule" class="ef-input" rows="2" placeholder='{"orgIds":[2]}'></textarea>
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
