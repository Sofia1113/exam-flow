import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
// 管理后台:开发端口 5174,接口经网关(8080)转发
export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5174,
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true
            }
        }
    }
});
