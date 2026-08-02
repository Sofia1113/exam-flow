<script setup lang="ts">
/**
 * 考试管理(骨架):考试计划、报名审核、排考场次。
 * TODO: 接入 registration-service 与 exam-service,含监考台入口。
 */
const plans = [
  { id: 1, name: '2026 年度职称认定考试', subject: '综合能力', status: '报名中', registered: 1280, capacity: 5000 },
  { id: 2, name: '2026 年下半年岗位能力测评', subject: '岗位技能', status: '组卷中', registered: 0, capacity: 3000 }
]
</script>

<template>
  <div>
    <h1 class="ef-page-title">考试管理</h1>

    <div class="ef-card" style="padding: 16px 24px; display: flex; justify-content: flex-end">
      <button class="ef-btn ef-btn-primary">创建考试计划</button>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>考试名称</th>
            <th>科目</th>
            <th>状态</th>
            <th class="num">已报名/容量</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="plan in plans" :key="plan.id">
            <td>{{ plan.name }}</td>
            <td>{{ plan.subject }}</td>
            <td>
              <span class="ef-status" :class="plan.status === '报名中' ? 'is-success' : 'is-warning'">{{ plan.status }}</span>
            </td>
            <td class="num">{{ plan.registered }} / {{ plan.capacity }}</td>
            <td>
              <button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px">报名审核</button>
              <button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px">排考</button>
              <button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px">监考台</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
