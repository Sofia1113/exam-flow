import { createRouter, createWebHistory } from 'vue-router'

/**
 * 考生门户路由。
 * 页面视觉遵循 DESIGN.md 政企风格;在线考试跳转独立工程 exam-client(全屏)。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
    { path: '/apply', name: 'apply', component: () => import('../views/ApplyView.vue'), meta: { title: '考试报名' } },
    { path: '/exams', name: 'exams', component: () => import('../views/ExamsView.vue'), meta: { title: '我的考试' } },
    { path: '/scores', name: 'scores', component: () => import('../views/ScoreView.vue'), meta: { title: '成绩查询' } }
  ]
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 国家考试信息网` : '国家考试信息网'
})

export default router
