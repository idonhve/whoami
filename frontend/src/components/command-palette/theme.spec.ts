import { beforeEach, describe, expect, it } from 'vitest'

import {
  THEME_KEY,
  applyTheme,
  cycleTheme,
  initTheme,
} from '@/components/command-palette/theme'

describe('theme 命令（Spec 10）', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-cmd-theme')
  })

  it('applyTheme(cyan) 设置 html 属性并持久化', () => {
    applyTheme('cyan')
    expect(document.documentElement.getAttribute('data-cmd-theme')).toBe('cyan')
    expect(localStorage.getItem(THEME_KEY)).toBe('cyan')
  })

  it('applyTheme(green) 移除属性回到 :root 默认', () => {
    applyTheme('cyan')
    applyTheme('green')
    expect(document.documentElement.getAttribute('data-cmd-theme')).toBeNull()
    expect(localStorage.getItem(THEME_KEY)).toBe('green')
  })

  it('cycleTheme 顺序循环 green → cyan → magenta → green', () => {
    expect(cycleTheme('green')).toBe('cyan')
    expect(cycleTheme('cyan')).toBe('magenta')
    expect(cycleTheme('magenta')).toBe('green')
  })

  it('initTheme 恢复上次主题', () => {
    localStorage.setItem(THEME_KEY, 'magenta')
    expect(initTheme()).toBe('magenta')
    expect(document.documentElement.getAttribute('data-cmd-theme')).toBe('magenta')
  })

  it('initTheme 对非法存储值回退 green', () => {
    localStorage.setItem(THEME_KEY, 'purple')
    expect(initTheme()).toBe('green')
    expect(document.documentElement.getAttribute('data-cmd-theme')).toBeNull()
  })

  it('主题样式注入一次且全部由既有 token 派生（无新色值）', () => {
    applyTheme('cyan')
    const style = document.getElementById('whoami-cmd-theme-style')
    expect(style).not.toBeNull()
    expect(style?.textContent).toContain('var(--cyan)')
    expect(style?.textContent).toContain('var(--magenta)')
    expect(style?.textContent).toContain('color-mix')
    expect(style?.textContent).not.toMatch(/#[0-9a-f]{3,8}/i)
  })
})
