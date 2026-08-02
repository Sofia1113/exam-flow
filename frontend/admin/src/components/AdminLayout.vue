<script setup lang="ts">
/**
 * 管理后台布局:深藏青顶栏 + 白色侧边导航(当前项红竖线 + 红字)+ 内容区。
 * 依据 DESIGN.md:深藏青结构色、1px 细线、黑体导航。
 */
const menus = [
  { path: '/', label: '工作台' },
  { path: '/questions', label: '题库管理' },
  { path: '/exams', label: '考试管理' },
  { path: '/scores', label: '成绩管理' }
]

const isActive = (path: string) => (path === '/' ? location.pathname === '/' : location.pathname.startsWith(path))
</script>

<template>
  <div>
    <header class="ef-admin-topbar">
      <span class="ef-logo">考试管理系统</span>
      <span class="ef-user">管理员:×××</span>
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
