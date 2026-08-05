import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// base 与后端 context-path (/api) 对齐，构建产物可直接放进 Spring Boot 静态目录
export default defineConfig({
  base: '/api/',
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
