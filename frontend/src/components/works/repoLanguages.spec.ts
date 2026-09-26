import { describe, expect, it } from 'vitest'

import { langColor, relativeTime } from './repoLanguages'

describe('语言色点映射', () => {
  it('常见语言映射到对应 token', () => {
    expect(langColor('TypeScript')).toBe('var(--lang-ts)')
    expect(langColor('Java')).toBe('var(--lang-java)')
    expect(langColor('Go')).toBe('var(--lang-go)')
    expect(langColor('Vue')).toBe('var(--lang-vue)')
  })

  it('未知语言与空值回退 fallback token（不裸写色值）', () => {
    expect(langColor('COBOL')).toBe('var(--lang-fallback)')
    expect(langColor(null)).toBe('var(--lang-fallback)')
    expect(langColor('')).toBe('var(--lang-fallback)')
  })
})

describe('相对更新时间', () => {
  it('分钟/小时/天/月档位', () => {
    expect(relativeTime(null)).toBe('-')
    expect(relativeTime(new Date().toISOString())).toMatch(/just now|now/)
    expect(relativeTime(new Date(Date.now() - 30 * 60_000).toISOString())).toBe('30m ago')
    expect(relativeTime(new Date(Date.now() - 5 * 3600_000).toISOString())).toBe('5h ago')
    expect(relativeTime(new Date(Date.now() - 3 * 86400_000).toISOString())).toBe('3d ago')
    expect(relativeTime(new Date(Date.now() - 40 * 86400_000).toISOString())).toBe('1mo ago')
  })

  it('非法时间与未来时间', () => {
    expect(relativeTime('not-a-date')).toBe('-')
    expect(relativeTime(new Date(Date.now() + 60_000).toISOString())).toBe('now')
  })
})
