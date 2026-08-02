import { createRouter, createWebHistory } from 'vue-router'

/**
 * 管理后台路由(骨架,信息架构见 TDD §6.2)。
 * TODO: 登录鉴权守卫、按角色动态过滤菜单。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '工作台' } },
    { path: '/questions', name: 'questions', component: () => import('../views/QuestionsView.vue'), meta: { title: '题库管理' } },
    { path: '/exams', name: 'exams', component: () => import('../views/ExamsView.vue'), meta: { title: '考试管理' } },
    { path: '/scores', name: 'scores', component: () => import('../views/ScoresView.vue'), meta: { title: '成绩管理' } }
  ]
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 考试管理系统` : '考试管理系统'
})

export default router
