import { defineStore } from 'pinia'
import request from '../api/request'

/**
 * 考生认证状态(与 admin 共用 localStorage 令牌键,考试端 URL 传递)。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('examflow_token') || '',
    username: localStorage.getItem('examflow_username') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(username: string, password: string) {
      const data = await request.post('/auth/login', { username, password })
      this.setToken(data)
    },
    async smsLogin(phone: string, code: string) {
      const data = await request.post('/auth/sms/login', { phone, code })
      this.setToken(data)
    },
    setToken(data: { accessToken: string; username: string }) {
      this.token = data.accessToken
      this.username = data.username
      localStorage.setItem('examflow_token', data.accessToken)
      localStorage.setItem('examflow_username', data.username)
    },
    logout() {
      this.token = ''
      this.username = ''
      localStorage.removeItem('examflow_token')
      localStorage.removeItem('examflow_username')
    }
  }
})
