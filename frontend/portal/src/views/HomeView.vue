<script setup lang="ts">
/**
 * 首页:红色横幅 + 公告列表(骨架,数据待接报名排考服务接口)。
 */
const announcements = [
  { id: 1, title: '2026 年度职称认定考试报名公告', date: '2026-08-01' },
  { id: 2, title: '2026 年下半年专业技术人员职业资格考试安排', date: '2026-07-20' },
  { id: 3, title: '关于在线考试系统升级维护的通知', date: '2026-07-10' }
]
</script>

<template>
  <div>
    <section class="ef-banner">
      <div class="ef-container">
        <h1>2026 年度职称认定考试报名系统</h1>
        <p>报名时间:2026-08-01 09:00 至 2026-08-31 17:00,请考生在规定时间内完成报名。</p>
      </div>
    </section>

    <section>
      <h2 class="ef-section-title">
        通知公告
        <router-link to="/apply" class="ef-btn ef-btn-outline" style="font-size: 14px; padding: 6px 16px">进入报名</router-link>
      </h2>
      <ul class="ef-announce-list">
        <li v-for="item in announcements" :key="item.id">
          <a href="#">{{ item.title }}</a>
          <time>{{ item.date }}</time>
        </li>
      </ul>
    </section>
  </div>
</template>
