<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

/**
 * 用户管理:分页列表(数据权限隔离)+ 创建用户 + 状态变更。
 */
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const roles = ref<any[]>([])

const showCreate = ref(false)
const form = reactive({ username: '', password: '', name: '', phone: '', orgId: 0, userType: 'internal', roleIds: [] as number[] })

async function load() {
  const data = await request.get('/users', { params: { page: page.value, size, keyword: keyword.value || undefined } })
  list.value = data.list
  total.value = data.total
}

async function loadRoles() {
  roles.value = await request.get('/roles/all')
}

async function create() {
  await request.post('/users', { ...form })
  showCreate.value = false
  Object.assign(form, { username: '', password: '', name: '', phone: '', orgId: 0, userType: 'internal', roleIds: [] })
  await load()
}

async function changeStatus(user: any) {
  const next = user.status === 'enabled' ? 'disabled' : 'enabled'
  await request.put(`/users/${user.id}/status`, null, { params: { status: next } })
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadRoles()])
})
</script>

<template>
  <div>
    <h1 class="ef-page-title">用户管理</h1>

    <div class="ef-card" style="display: flex; justify-content: space-between; align-items: center; padding: 16px 24px">
      <input v-model="keyword" class="ef-input" style="width: 240px" placeholder="按用户名/姓名检索" @keyup.enter="page = 1; load()" />
      <div>
        <button class="ef-btn ef-btn-plain" style="margin-right: 8px" @click="page = 1; load()">查询</button>
        <button class="ef-btn ef-btn-primary" @click="showCreate = true">创建用户</button>
      </div>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>账号</th>
            <th>姓名</th>
            <th>手机号</th>
            <th>组织</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in list" :key="u.id">
            <td>{{ u.username }}</td>
            <td>{{ u.name }}</td>
            <td>{{ u.phoneMasked || '-' }}</td>
            <td>{{ u.orgName || '-' }}</td>
            <td>{{ u.roles.join(', ') || '-' }}</td>
            <td><span class="ef-status" :class="u.status === 'enabled' ? 'is-success' : 'is-error'">{{ u.status }}</span></td>
            <td>
              <button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px" @click="changeStatus(u)">
                {{ u.status === 'enabled' ? '停用' : '启用' }}
              </button>
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

    <!-- 创建用户弹窗 -->
    <div v-if="showCreate" class="modal-mask">
      <div class="ef-card modal">
        <h2 style="font-family: var(--ef-font-heiti); font-size: 18px; margin-bottom: 16px">创建用户</h2>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">账号</label>
          <input v-model="form.username" class="ef-input" /></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">密码(≥10 位,含三类字符)</label>
          <input v-model="form.password" type="password" class="ef-input" /></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">姓名</label>
          <input v-model="form.name" class="ef-input" /></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label">手机号</label>
          <input v-model="form.phone" class="ef-input" /></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label required">组织 ID</label>
          <input v-model.number="form.orgId" type="number" class="ef-input" /></div>
        <div style="margin-bottom: 12px"><label class="ef-form-label">角色</label>
          <select v-model="form.roleIds" multiple class="ef-input">
            <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</option>
          </select></div>
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
  width: 480px;
  border-top: 3px solid var(--ef-primary);
}
</style>
