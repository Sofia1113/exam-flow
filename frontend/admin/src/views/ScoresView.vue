<script setup lang="ts">
/**
 * 成绩管理(骨架):评阅进度、成绩发布、公示与更正。
 * TODO: 接入 grading-service,评阅任务分派、双评仲裁、成绩发布流程。
 */
const batches = [
  { exam: '2026 年度职称认定考试', graded: 0, total: 1280, status: '评阅中' },
  { exam: '2026 年上半年岗位能力测评', graded: 850, total: 850, status: '公示中' }
]
</script>

<template>
  <div>
    <h1 class="ef-page-title">成绩管理</h1>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>考试名称</th>
            <th class="num">已评阅/总数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="batch in batches" :key="batch.exam">
            <td>{{ batch.exam }}</td>
            <td class="num">{{ batch.graded }} / {{ batch.total }}</td>
            <td>
              <span class="ef-status" :class="batch.status === '公示中' ? 'is-success' : 'is-warning'">{{ batch.status }}</span>
            </td>
            <td>
              <button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px">评阅管理</button>
              <button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px">成绩发布</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
