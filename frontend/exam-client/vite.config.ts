import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 在线考试端:开发端口 5175,全屏应用;接口经网关(8080)转发
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5175,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
