import { createRouter, createWebHistory } from 'vue-router'

/**
 * 管理后台路由:登录守卫(未登录跳 /login)。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { title: '登录', public: true } },
    { path: '/', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '工作台' } },
    { path: '/users', name: 'users', component: () => import('../views/UsersView.vue'), meta: { title: '用户管理' } },
    { path: '/roles', name: 'roles', component: () => import('../views/RolesView.vue'), meta: { title: '角色权限' } },
    { path: '/questions', name: 'questions', component: () => import('../views/QuestionsView.vue'), meta: { title: '题库管理' } },
    { path: '/papers', name: 'papers', component: () => import('../views/PapersView.vue'), meta: { title: '试卷管理' } },
    { path: '/exams', name: 'exams', component: () => import('../views/ExamsView.vue'), meta: { title: '考试管理' } },
    { path: '/scores', name: 'scores', component: () => import('../views/ScoresView.vue'), meta: { title: '成绩管理' } }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('examflow_token')
  if (!to.meta.public && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: '/' }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 考试管理系统` : '考试管理系统'
})

export default router
