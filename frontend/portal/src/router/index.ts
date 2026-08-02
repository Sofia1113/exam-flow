import { createRouter, createWebHistory } from 'vue-router'

/**
 * 考生门户路由。
 * 页面视觉遵循 DESIGN.md 政企风格;在线考试跳转独立工程 exam-client(全屏)。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { title: '登录', public: true } },
    { path: '/apply', name: 'apply', component: () => import('../views/ApplyView.vue'), meta: { title: '考试报名', requiresAuth: true } },
    { path: '/exams', name: 'exams', component: () => import('../views/ExamsView.vue'), meta: { title: '我的考试', requiresAuth: true } },
    { path: '/scores', name: 'scores', component: () => import('../views/ScoreView.vue'), meta: { title: '成绩查询' } }
  ]
})

// 登录守卫:报名/考试需登录
router.beforeEach((to) => {
  const token = localStorage.getItem('examflow_token')
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 国家考试信息网` : '国家考试信息网'
})

export default router
