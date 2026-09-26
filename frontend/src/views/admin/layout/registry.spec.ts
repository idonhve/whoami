import { describe, expect, it } from 'vitest'

import { adminChildRoutes, adminModules, adminNavItems } from './registry'

describe('后台导航聚合表（挂载约定）', () => {
  it('每个导航项的 name 都能找到对应路由', () => {
    const routeNames = new Set(adminChildRoutes.map((r) => r.name))
    for (const item of adminNavItems) {
      expect(routeNames.has(item.name), `导航项 ${item.name} 缺少对应路由`).toBe(true)
    }
  })

  it('子路由 path 与 name 均不重复（并行窗口零冲突）', () => {
    const paths = adminChildRoutes.map((r) => r.path)
    const names = adminChildRoutes.map((r) => r.name)
    expect(new Set(paths).size).toBe(paths.length)
    expect(new Set(names).size).toBe(names.length)
  })

  it('导航项带 16x16 SVG path 图标（不用 emoji）', () => {
    for (const item of adminNavItems) {
      expect(item.icon.length).toBeGreaterThan(0)
      expect(item.icon).toMatch(/^[Mm0-9\s.,HLVZhlvz-]+$/)
    }
  })

  it('F6 交付的模块已挂载：站点配置 + 操作日志', () => {
    const names = adminModules.flatMap((m) => m.nav.map((n) => n.name))
    expect(names).toContain('admin-config')
    expect(names).toContain('admin-op-logs')
  })

  it('F4 交付的模块已挂载：作品管理', () => {
    const names = adminModules.flatMap((m) => m.nav.map((n) => n.name))
    expect(names).toContain('admin-works')
  })
})
