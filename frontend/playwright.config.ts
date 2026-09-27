import { defineConfig, devices } from '@playwright/test'

/**
 * 前端 E2E（Spec 09 经历模块）。
 * 运行前置：全栈已起（docker compose up），/experience 数据来自 experience 表；
 * 本地默认通过 vite preview 的 /api 代理命中后端（见 vite.config.ts proxy）。
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'desktop-chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'mobile-chromium',
      use: { ...devices['Pixel 7'] },
    },
  ],
})
