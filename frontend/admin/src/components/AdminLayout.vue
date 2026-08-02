<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/**
 * 管理后台布局:深藏青顶栏 + 白色侧边导航(当前项红竖线 + 红字)+ 内容区。
 * 依据 DESIGN.md:深藏青结构色、1px 细线、黑体导航。
 */
const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/login')
}
const menus = [
  { path: '/', label: '工作台' },
  { path: '/users', label: '用户管理' },
  { path: '/roles', label: '角色权限' },
  { path: '/questions', label: '题库管理' },
  { path: '/papers', label: '试卷管理' },
  { path: '/exams', label: '考试管理' },
  { path: '/grading', label: '阅卷管理' },
  { path: '/scores', label: '成绩管理' }
]

const isActive = (path: string) => (path === '/' ? location.pathname === '/' : location.pathname.startsWith(path))
</script>

<template>
  <div>
    <header class="ef-admin-topbar">
      <span class="ef-logo">考试管理系统</span>
      <span class="ef-user">
        {{ auth.username }}
        <a href="#" style="color: var(--ef-inverse-ink-muted); margin-left: 12px" @click.prevent="logout">退出</a>
      </span>
    </header>

    <aside class="ef-admin-sidebar">
      <router-link v-for="menu in menus" :key="menu.path" :to="menu.path" class="ef-menu-item"
                   :class="{ 'is-active': isActive(menu.path) }">
        {{ menu.label }}
      </router-link>
    </aside>

    <main class="ef-admin-main">
      <slot />
    </main>
  </div>
</template>
