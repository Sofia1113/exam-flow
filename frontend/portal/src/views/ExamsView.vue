<script setup lang="ts">
/**
 * 我的考试(骨架):场次、准考证、在线考试入口。
 * 在线考试跳转独立工程 exam-client(全屏应用,带防作弊环境锁定)。
 */
const myExams = [
  { id: 1, name: '2026 年度职称认定考试', slot: '2026-09-15 09:00-11:00', ticketNo: '2026-00001', status: '待考试' }
]
</script>

<template>
  <div>
    <h1 class="ef-section-title">我的考试</h1>

    <table class="ef-table">
      <thead>
        <tr>
          <th>考试名称</th>
          <th>场次时间</th>
          <th>准考证号</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="exam in myExams" :key="exam.id">
          <td>{{ exam.name }}</td>
          <td>{{ exam.slot }}</td>
          <td>{{ exam.ticketNo }}</td>
          <td><span class="ef-status is-warning">{{ exam.status }}</span></td>
          <td>
            <button class="ef-btn ef-btn-primary" style="padding: 6px 16px; font-size: 14px" disabled>
              进入考试(开发中)
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <p style="color: var(--ef-ink-muted); margin-top: 16px">
      考试须知:请于开考前 30 分钟至开考后 30 分钟内进入考试;迟到 30 分钟禁止入场。考试期间请保持网络畅通,断线 5 分钟内可恢复继续作答。
    </p>
  </div>
</template>
