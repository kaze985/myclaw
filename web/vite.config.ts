import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// base 区分环境：
// - 开发（vite dev）：base='/'，页面与资源由 Vite 自身提供，/api 走 proxy 到后端
// - 生产（vite build）：base='/api/'，与后端 context-path 对齐，产物放进 Spring Boot 静态目录
export default defineConfig(({ command }) => ({
  base: command === 'build' ? '/api/' : '/',
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
}))
