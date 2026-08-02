<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/**
 * 门户头部:深藏青通栏(36px)+ 白色主导航(64px,当前项红底白字)。
 * 依据 DESIGN.md §Components 顶部结构。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/')
}

const navItems = [
  { path: '/', label: '首页' },
  { path: '/apply', label: '考试报名' },
  { path: '/exams', label: '我的考试' },
  { path: '/scores', label: '成绩查询' }
]

const isActive = (path: string) => (path === '/' ? route.path === '/' : route.path.startsWith(path))

const today = computed(() => new Date().toLocaleDateString('zh-CN'))
</script>

<template>
  <header>
    <div class="ef-top-rail">
      <div class="ef-container">
        <span>国家考试信息网</span>
        <span class="ef-top-rail-right">
          {{ today }}
          <template v-if="auth.isLoggedIn">
            <span style="margin-left: 16px">{{ auth.username }}</span>
            <a href="#" style="margin-left: 12px" @click.prevent="logout">退出</a>
          </template>
          <router-link v-else to="/login" style="margin-left: 16px">登录</router-link>
        </span>
      </div>
    </div>
    <nav class="ef-nav">
      <div class="ef-container">
        <router-link to="/" class="ef-nav-logo">国家考试信息网</router-link>
        <div class="ef-nav-items">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="ef-nav-item"
            :class="{ 'is-active': isActive(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </div>
      </div>
    </nav>
  </header>
</template>
