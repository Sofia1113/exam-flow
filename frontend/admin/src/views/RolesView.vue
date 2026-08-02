<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

/**
 * 角色权限管理:角色列表 + 权限码分配 + 数据权限范围。
 */
const list = ref<any[]>([])
const showPerm = ref(false)
const current = ref<any>(null)
const permInput = ref('')
const scopeForm = reactive({ scopeType: 'all', orgIds: '' })

async function load() {
  list.value = await request.get('/roles/all')
}

async function openPerm(role: any) {
  current.value = role
  permInput.value = role.permCodes.join(', ')
  scopeForm.scopeType = role.scopeType || 'all'
  scopeForm.orgIds = role.scopeOrgIds || ''
  showPerm.value = true
}

async function savePerm() {
  const permCodes = permInput.value.split(',').map((s) => s.trim()).filter(Boolean)
  await request.put(`/roles/${current.value.id}/perms`, { permCodes })
  await request.put(`/roles/${current.value.id}/data-scope`, { ...scopeForm })
  showPerm.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="ef-page-title">角色权限</h1>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>角色编码</th>
            <th>名称</th>
            <th>权限码</th>
            <th>数据范围</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>{{ r.code }}</td>
            <td>{{ r.name }}</td>
            <td style="max-width: 360px">{{ r.permCodes.join(', ') || '-' }}</td>
            <td>{{ r.scopeType || '-' }}</td>
            <td><button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px" @click="openPerm(r)">配置权限</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showPerm && current" class="modal-mask">
      <div class="ef-card modal">
        <h2 style="font-family: var(--ef-font-heiti); font-size: 18px; margin-bottom: 16px">
          权限配置 · {{ current.name }}
        </h2>
        <div style="margin-bottom: 12px">
          <label class="ef-form-label">权限码(逗号分隔,如 question:edit,question:audit)</label>
          <input v-model="permInput" class="ef-input" />
        </div>
        <div style="margin-bottom: 12px">
          <label class="ef-form-label">数据权限范围</label>
          <select v-model="scopeForm.scopeType" class="ef-input">
            <option value="all">全部组织</option>
            <option value="current">本级组织</option>
            <option value="children">本级及下级</option>
          </select>
        </div>
        <div style="text-align: right">
          <button class="ef-btn ef-btn-plain" @click="showPerm = false">取消</button>
          <button class="ef-btn ef-btn-primary" style="margin-left: 8px" @click="savePerm">保存</button>
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
  width: 520px;
  border-top: 3px solid var(--ef-primary);
}
</style>
