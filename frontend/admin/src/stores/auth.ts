import { defineStore } from 'pinia'
import request from '../api/request'

/**
 * 认证状态:令牌与当前用户权限(登录后从 /users/permissions/current 拉取)。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('examflow_token') || '',
    username: localStorage.getItem('examflow_username') || '',
    perms: [] as string[]
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    hasPerm: (state) => (perm: string) => state.perms.includes('*') || state.perms.includes(perm)
  },
  actions: {
    async login(username: string, password: string) {
      const data = await request.post('/auth/login', { username, password })
      this.token = data.accessToken
      this.username = data.username
      localStorage.setItem('examflow_token', data.accessToken)
      localStorage.setItem('examflow_username', data.username)
      await this.loadPerms()
    },
    async loadPerms() {
      try {
        const data = await request.get('/users/permissions/current')
        this.perms = data.permCodes || []
      } catch {
        this.perms = []
      }
    },
    logout() {
      this.token = ''
      this.username = ''
      this.perms = []
      localStorage.removeItem('examflow_token')
      localStorage.removeItem('examflow_username')
    }
  }
})
