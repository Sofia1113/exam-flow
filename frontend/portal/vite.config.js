import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
// 考生门户:开发端口 5173,接口经网关(8080)转发
export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true
            }
        }
    }
});
