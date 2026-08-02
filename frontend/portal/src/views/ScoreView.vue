<script setup lang="ts">
/**
 * 成绩查询(骨架):公示期、申诉入口、成绩单与证书下载。
 */
const scores = [
  {
    exam: '2026 年度职称认定考试',
    total: 0,
    objective: 0,
    subjective: 0,
    pass: false,
    status: '成绩未发布'
  }
]
</script>

<template>
  <div>
    <h1 class="ef-section-title">成绩查询</h1>

    <table class="ef-table">
      <thead>
        <tr>
          <th>考试名称</th>
          <th class="num">总分</th>
          <th class="num">客观题</th>
          <th class="num">主观题</th>
          <th>是否合格</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="score in scores" :key="score.exam">
          <td>{{ score.exam }}</td>
          <td class="num">{{ score.total }}</td>
          <td class="num">{{ score.objective }}</td>
          <td class="num">{{ score.subjective }}</td>
          <td><span class="ef-status" :class="score.pass ? 'is-success' : 'is-error'">-</span></td>
          <td><span class="ef-status is-warning">{{ score.status }}</span></td>
          <td><button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 14px" disabled>申诉</button></td>
        </tr>
      </tbody>
    </table>

    <p style="color: var(--ef-ink-muted); margin-top: 16px">
      成绩公布后设 3 个工作日公示期,公示期内可提出成绩异议申诉。
    </p>
  </div>
</template>
