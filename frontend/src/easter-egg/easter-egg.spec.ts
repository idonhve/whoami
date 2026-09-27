import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { MockInstance } from 'vitest'

import { trackEasterEgg } from '@/api/track'
import { initEasterEgg } from '@/easter-egg'
import { EASTER_EGG_COPY } from '@/easter-egg/consoleArt'
import { KONAMI_SEQUENCE, createKonamiMatcher } from '@/easter-egg/konami'
import ParticleRain from '@/easter-egg/ParticleRain.vue'
import { prefersReducedMotion } from '@/utils/motion'

vi.mock('@/api/track', () => ({
  trackEasterEgg: vi.fn(),
}))

vi.mock('@/utils/motion', () => ({
  prefersReducedMotion: vi.fn(() => false),
}))

// 匹配器工厂打桩仅为可数（零开销断言：未进入候选状态时不被创建/调用），
// 匹配行为仍透传真实现。
vi.mock('@/easter-egg/konami', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/easter-egg/konami')>()
  return { ...actual, createKonamiMatcher: vi.fn(actual.createKonamiMatcher) }
})

const trackEasterEggMock = vi.mocked(trackEasterEgg)
const prefersReducedMotionMock = vi.mocked(prefersReducedMotion)
const createKonamiMatcherMock = vi.mocked(createKonamiMatcher)

/** 完整秘籍序列（KONAMI_SEQUENCE 的可分发副本） */
const SEQ: string[] = [...KONAMI_SEQUENCE]

/** 以真实 KeyboardEvent 依次按键 */
function press(keys: string[]): void {
  for (const key of keys) {
    window.dispatchEvent(new KeyboardEvent('keydown', { key }))
  }
}

describe('F11 控制台彩蛋 + 键盘秘籍（Spec 11）', () => {
  let logSpy: MockInstance

  beforeEach(() => {
    vi.clearAllMocks()
    prefersReducedMotionMock.mockReturnValue(false)
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => {})
    document.body.innerHTML = ''
  })

  afterEach(() => {
    logSpy.mockRestore()
    document.body.innerHTML = ''
  })

  it('应用挂载后 console 输出一次彩蛋：ASCII 签名 + 招聘联系方式 + "对的人"文案', () => {
    initEasterEgg()

    expect(logSpy).toHaveBeenCalledTimes(1)
    const output = logSpy.mock.calls[0][0] as string
    expect(output).toContain(EASTER_EGG_COPY.ascii)
    expect(output).toContain('你打开了控制台，说明你是对的人')
    expect(output).toContain('https://github.com/idonhve')
    expect(output).toContain('2155539871@qq.com')
  })

  it('完整秘籍序列触发：easter_egg 埋点 + toast + 粒子雨挂载，随后自动清理', async () => {
    initEasterEgg()

    press(SEQ)

    expect(trackEasterEggMock).toHaveBeenCalledTimes(1)
    expect(trackEasterEggMock).toHaveBeenCalledWith('konami')
    expect(document.querySelector('.konami-toast')).not.toBeNull()
    expect(document.querySelector('canvas.konami-rain')).not.toBeNull()

    // happy-dom 无 2D context：组件走防御性完成路径，宿主节点被自动移除
    await vi.waitFor(() => {
      expect(document.querySelector('canvas.konami-rain')).toBeNull()
    })
  })

  it('prefers-reduced-motion：粒子雨降级为仅 toast', () => {
    prefersReducedMotionMock.mockReturnValue(true)
    initEasterEgg()

    press(SEQ)

    expect(trackEasterEggMock).toHaveBeenCalledTimes(1)
    expect(document.querySelector('.konami-toast')).not.toBeNull()
    expect(document.querySelector('canvas.konami-rain')).toBeNull()
  })

  it('连打两轮可触发两次（toast 替换不叠加）', () => {
    prefersReducedMotionMock.mockReturnValue(true)
    initEasterEgg()

    press(SEQ)
    press(SEQ)

    expect(trackEasterEggMock).toHaveBeenCalledTimes(2)
    expect(document.querySelectorAll('.konami-toast')).toHaveLength(1)
  })

  it('普通按键不触发，且匹配器未被创建/调用（零开销性能断言）', () => {
    initEasterEgg()

    press(['a', 'Shift', 'Enter', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'b', 'A', '1'])

    expect(trackEasterEggMock).not.toHaveBeenCalled()
    expect(document.querySelector('.konami-toast')).toBeNull()
    expect(createKonamiMatcherMock).not.toHaveBeenCalled()
  })

  it('错误序列中途重置：打断后完整序列恰好再触发一次', () => {
    prefersReducedMotionMock.mockReturnValue(true)
    initEasterEgg()

    press(['ArrowUp', 'ArrowUp', 'ArrowDown', 'x'])
    expect(trackEasterEggMock).not.toHaveBeenCalled()

    press(SEQ)
    expect(trackEasterEggMock).toHaveBeenCalledTimes(1)

    // 再次中途打断 → 仍可重新触发
    press(['ArrowUp', 'ArrowUp', 'ArrowDown', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'x'])
    press(SEQ)
    expect(trackEasterEggMock).toHaveBeenCalledTimes(2)
  })
})

describe('konami 纯序列匹配器', () => {
  it('非候选首键 O(1) 退出：不进入匹配状态', () => {
    const matcher = createKonamiMatcher()

    expect(matcher.press('a')).toBe('idle')
    expect(matcher.press('ArrowDown')).toBe('idle') // 属于序列但不是首键
    expect(matcher.progress).toBe(0)
  })

  it('完整序列返回 completed，且触发后重置可再次触发', () => {
    const matcher = createKonamiMatcher()

    const states = SEQ.map((key) => matcher.press(key))
    expect(states).toHaveLength(10)
    expect(states.at(-1)).toBe('completed')
    expect(matcher.progress).toBe(0)

    const again = SEQ.map((key) => matcher.press(key))
    expect(again.at(-1)).toBe('completed')
  })

  it('b / a 大小写不敏感（Shift 或大写锁定下同样命中）', () => {
    const matcher = createKonamiMatcher()

    const caps = [...SEQ.slice(0, 8), 'B', 'A']
    expect(caps.map((key) => matcher.press(key)).at(-1)).toBe('completed')
  })

  it('错误序列中途重置', () => {
    const matcher = createKonamiMatcher()

    matcher.press('ArrowUp')
    matcher.press('ArrowUp')
    expect(matcher.press('x')).toBe('idle')
    expect(matcher.progress).toBe(0)

    expect(SEQ.map((key) => matcher.press(key)).at(-1)).toBe('completed')
  })

  it('序列窗口期约 5 秒：期内可继续，超时后重置', () => {
    let t = 1_000
    const matcher = createKonamiMatcher(() => t)

    expect(matcher.press('ArrowUp')).toBe('matching')
    t = 6_000 // 恰好到窗口边界（elapsed = 5000）：未超时
    expect(matcher.press('ArrowUp')).toBe('matching')
    t = 6_001 // 超时：重置后 ↓ 不是首键 → 退出候选状态
    expect(matcher.press('ArrowDown')).toBe('idle')
    expect(matcher.progress).toBe(0)

    t = 7_000
    expect(matcher.press('ArrowUp')).toBe('matching')
    expect(SEQ.slice(1).map((key) => matcher.press(key)).at(-1)).toBe('completed')
  })
})

describe('ParticleRain 粒子雨组件', () => {
  it('挂载渲染 aria-hidden 全局画布；无 2D context 的环境走完成路径并通知宿主清理', async () => {
    prefersReducedMotionMock.mockReturnValue(false)
    const wrapper = mount(ParticleRain)

    const canvas = wrapper.find('canvas.konami-rain')
    expect(canvas.exists()).toBe(true)
    expect(canvas.attributes('aria-hidden')).toBe('true')

    await vi.waitFor(() => {
      expect(wrapper.emitted('finished')).toHaveLength(1)
    })
    wrapper.unmount()
  })

  it('prefers-reduced-motion：不渲染画布，立即完成（降级为仅 toast）', async () => {
    prefersReducedMotionMock.mockReturnValue(true)
    const wrapper = mount(ParticleRain)

    expect(wrapper.find('canvas').exists()).toBe(false)
    await vi.waitFor(() => {
      expect(wrapper.emitted('finished')).toHaveLength(1)
    })
    wrapper.unmount()
  })

  it('有 2D context 时：粒子绘制数秒后自动停止（rAF 停止）并通知宿主清理', async () => {
    prefersReducedMotionMock.mockReturnValue(false)
    const fillRect = vi.fn()
    const stubCtx = {
      clearRect: vi.fn(),
      scale: vi.fn(),
      fillRect,
    } as unknown as CanvasRenderingContext2D
    const proto = HTMLCanvasElement.prototype as unknown as {
      getContext: () => CanvasRenderingContext2D | null
    }
    const getContextSpy = vi.spyOn(proto, 'getContext').mockReturnValue(stubCtx)

    try {
      vi.useFakeTimers()
      const wrapper = mount(ParticleRain)

      // 推进若干帧：粒子正在绘制
      await vi.advanceTimersByTimeAsync(120)
      expect(fillRect.mock.calls.length).toBeGreaterThan(0)

      // 推进到总时长（约 4s）之后：自动消散并停止 rAF
      await vi.advanceTimersByTimeAsync(5_000)
      expect(wrapper.emitted('finished')).toHaveLength(1)

      fillRect.mockClear()
      await vi.advanceTimersByTimeAsync(600)
      expect(fillRect).not.toHaveBeenCalled()
      wrapper.unmount()
    } finally {
      vi.useRealTimers()
      getContextSpy.mockRestore()
    }
  })
})
