<script setup lang="ts">
/**
 * 题库管理(骨架):题目列表 + 状态流转(草稿/待审/已审/发布/停用)。
 * TODO: 接入 question-service,批量导入、审题操作。
 */
const questions = [
  { id: 'ZG-00123', type: '单选题', subject: '综合能力', difficulty: 3, status: '已审' },
  { id: 'ZG-00124', type: '多选题', subject: '综合能力', difficulty: 4, status: '待审' },
  { id: 'ZG-00125', type: '案例分析', subject: '岗位技能', difficulty: 5, status: '草稿' }
]
</script>

<template>
  <div>
    <h1 class="ef-page-title">题库管理</h1>

    <div class="ef-card" style="display: flex; justify-content: space-between; align-items: center; padding: 16px 24px">
      <div>
        <span class="ef-tag" style="margin-right: 8px">科目:综合能力</span>
        <span class="ef-tag">题型:全部</span>
      </div>
      <button class="ef-btn ef-btn-primary">批量导入</button>
    </div>

    <div class="ef-card" style="padding: 0">
      <table class="ef-table">
        <thead>
          <tr>
            <th>题号</th>
            <th>题型</th>
            <th>科目</th>
            <th class="num">难度</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in questions" :key="q.id">
            <td>{{ q.id }}</td>
            <td>{{ q.type }}</td>
            <td>{{ q.subject }}</td>
            <td class="num">{{ q.difficulty }}</td>
            <td><span class="ef-status" :class="q.status === '已审' ? 'is-success' : q.status === '待审' ? 'is-warning' : ''">{{ q.status }}</span></td>
            <td><button class="ef-btn ef-btn-plain" style="padding: 4px 12px; font-size: 13px">编辑</button></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
