import { createRouter, createWebHistory } from 'vue-router'

/**
 * 在线考试端路由:仅考试房间。
 * 由门户"进入考试"携带会话令牌跳转: /exam?token=xxx&sessionId=xxx
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/exam',
      name: 'exam-room',
      component: () => import('../views/ExamRoom.vue'),
      meta: { title: '在线考试' }
    },
    { path: '/:pathMatch(.*)*', redirect: '/exam' }
  ]
})

router.afterEach(() => {
  document.title = '在线考试'
})

export default router
