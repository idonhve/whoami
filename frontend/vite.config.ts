import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/admin/api': 'http://localhost:8080',
    },
  },
  build: {
    rollupOptions: {
      output: {
        // 重型依赖独立分包，保住首包体积红线（PRD §4.1：首包 gzip ≤ 500KB，不含 3D 场景）
        manualChunks(id: string) {
          if (id.includes('node_modules/three')) return 'three'
          if (id.includes('node_modules/gsap')) return 'gsap'
          if (id.includes('node_modules/echarts')) return 'echarts'
          if (id.includes('node_modules/naive-ui')) return 'naive-ui'
        },
      },
    },
  },
})
