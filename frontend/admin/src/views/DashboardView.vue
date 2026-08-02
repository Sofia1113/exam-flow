<script setup lang="ts">
/**
 * 工作台(骨架):统计卡(顶部 3px 红边线,依据 DESIGN.md stat-card)+ 待办。
 * TODO: 接入 report-service 考次总览与 sys-service 待办。
 */
const stats = [
  { label: '进行中考试', value: '3' },
  { label: '今日在线考生', value: '0' },
  { label: '待审核报名', value: '12' },
  { label: '待评阅答卷', value: '0' }
]

const todos = [
  { name: '2026 年度职称认定考试报名审核', count: 12, link: '去审核' },
  { name: '2026 年下半年岗位能力测评组卷', count: 1, link: '去组卷' }
]
</script>

<template>
  <div>
    <h1 class="ef-page-title">工作台</h1>

    <div class="ef-grid-4">
      <div v-for="stat in stats" :key="stat.label" class="ef-stat-card">
        <div class="ef-stat-number">{{ stat.value }}</div>
        <div class="ef-stat-label">{{ stat.label }}</div>
      </div>
    </div>

    <div class="ef-card">
      <h2 style="font-family: var(--ef-font-heiti); font-size: 18px; margin-bottom: 12px">待办事项</h2>
      <table class="ef-table">
        <thead>
          <tr>
            <th>事项</th>
            <th class="num">数量</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="todo in todos" :key="todo.name">
            <td>{{ todo.name }}</td>
            <td class="num">{{ todo.count }}</td>
            <td><span class="ef-status is-warning">{{ todo.link }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
