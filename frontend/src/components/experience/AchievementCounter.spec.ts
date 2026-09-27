import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import AchievementCounter from './AchievementCounter.vue'

/** 拦截 gsap.to：不真正播放动画，暴露目标与回调供测试推进（不测 GSAP 内部）。 */
const toMock = vi.fn()
vi.mock('gsap', () => ({
  gsap: { to: (target: { v: number }, vars: { onComplete?: () => void }) => toMock(target, vars) },
}))

function stubMatchMedia(matches: Record<string, boolean>) {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches: Boolean(matches[query]),
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  )
}

describe('战果数字翻牌 AchievementCounter', () => {
  beforeEach(() => {
    toMock.mockClear()
    // happy-dom 自带不触发的 IntersectionObserver：置为 undefined 走 useInView 的"直接可见"分支，
    // 让进入视口触发动画的路径在单测里可复现。
    const win = window as { IntersectionObserver?: unknown; matchMedia?: unknown }
    win.IntersectionObserver = undefined
    win.matchMedia = undefined
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('纯文本值（无前导数字）直接显示，不动画', async () => {
    stubMatchMedia({ '(prefers-reduced-motion: reduce)': false })
    const wrapper = mount(AchievementCounter, { props: { value: '主导核心重构' } })
    await nextTick()
    expect(wrapper.find('[data-testid="achievement-counter"]').text()).toBe('主导核心重构')
    expect(toMock).not.toHaveBeenCalled()
  })

  it('数字递增：动画前为 0+后缀，动画完成后位完整值（300%）', async () => {
    stubMatchMedia({ '(prefers-reduced-motion: reduce)': false })
    const wrapper = mount(AchievementCounter, { props: { value: '300%' } })
    await nextTick()

    // before：进入视口已触发 gsap.to，但数字尚未递增 → 0%
    expect(toMock).toHaveBeenCalledTimes(1)
    const [, vars] = toMock.mock.calls[0]
    expect(wrapper.find('[data-testid="achievement-counter"]').text()).toBe('0%')

    // after：动画完成回调 → 300%，后缀 % 常显
    vars.onComplete?.()
    await nextTick()
    expect(wrapper.find('[data-testid="achievement-counter"]').text()).toBe('300%')
  })

  it('带单位 50w+：数字递增 + 后缀 w+ 常显', async () => {
    stubMatchMedia({ '(prefers-reduced-motion: reduce)': false })
    const wrapper = mount(AchievementCounter, { props: { value: '50w+' } })
    await nextTick()

    expect(wrapper.find('[data-testid="achievement-counter"]').text()).toBe('0w+')

    const [, vars] = toMock.mock.calls[0]
    vars.onComplete?.()
    await nextTick()
    expect(wrapper.find('[data-testid="achievement-counter"]').text()).toBe('50w+')
  })

  it('reduced-motion：直出最终值，跳过递增动画', async () => {
    stubMatchMedia({ '(prefers-reduced-motion: reduce)': true })
    const wrapper = mount(AchievementCounter, { props: { value: '300%' } })
    await nextTick()
    expect(wrapper.find('[data-testid="achievement-counter"]').text()).toBe('300%')
    expect(toMock).not.toHaveBeenCalled()
  })
})
