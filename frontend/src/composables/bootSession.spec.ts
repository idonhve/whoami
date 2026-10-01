import { beforeEach, describe, expect, it } from 'vitest'

import {
  BOOT_MUTED_KEY,
  BOOT_SEEN_KEY,
  BOOT_SEEN_TTL_MS,
  loadBootMuted,
  markBootSeen,
  saveBootMuted,
  shouldPlayBoot,
} from '@/composables/bootSession'

describe('开机动画会话状态', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('无标记时播放开机动画', () => {
    expect(shouldPlayBoot()).toBe(true)
  })

  it('24h 内的有效标记跳过开机动画', () => {
    const now = Date.now()
    markBootSeen(now)
    expect(shouldPlayBoot(now)).toBe(false)
  })

  it('标记超过 24h 自动失效并重播', () => {
    const now = Date.now()
    markBootSeen(now - BOOT_SEEN_TTL_MS - 1000)
    expect(shouldPlayBoot(now)).toBe(true)
  })

  it('非法标记按未访问处理', () => {
    localStorage.setItem(BOOT_SEEN_KEY, 'not-a-number')
    expect(shouldPlayBoot()).toBe(true)
  })

  it('静音偏好持久化', () => {
    expect(loadBootMuted()).toBe(false)
    saveBootMuted(true)
    expect(localStorage.getItem(BOOT_MUTED_KEY)).toBe('true')
    expect(loadBootMuted()).toBe(true)
    saveBootMuted(false)
    expect(loadBootMuted()).toBe(false)
  })
})
