import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { BOOT_SEEN_KEY } from '@/composables/bootSession'
import BootAnimation from '@/views/boot/BootAnimation.vue'

function stubMatchMedia(reduced: boolean) {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: reduced && query.includes('prefers-reduced-motion'),
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
    addListener: () => {},
    removeListener: () => {},
    onchange: null,
    dispatchEvent: () => false,
  }))
  window.matchMedia = globalThis.matchMedia as typeof window.matchMedia
}

function stubFetchFail() {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('no backend')))
}

function mountBoot(degraded = true) {
  return mount(BootAnimation, {
    props: { degraded },
    global: { plugins: [createPinia()] },
    attachTo: document.body,
  })
}

describe('开机动画组件', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    stubFetchFail()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.useRealTimers()
    document.body.innerHTML = ''
  })

  it('点击跳过：立即结束并写入 24h 跳过标记', async () => {
    stubMatchMedia(false)
    const wrapper = mountBoot()
    await wrapper.find('.boot-overlay').trigger('click')

    expect(wrapper.emitted('finished')).toHaveLength(1)
    expect(localStorage.getItem(BOOT_SEEN_KEY)).not.toBeNull()
    wrapper.unmount()
  })

  it('静音开关持久化偏好', async () => {
    stubMatchMedia(false)
    const wrapper = mountBoot()
    const btn = wrapper.find('.boot-mute')

    await btn.trigger('click')
    expect(localStorage.getItem('whoami:boot:muted')).toBe('true')
    expect(btn.attributes('aria-pressed')).toBe('true')

    await btn.trigger('click')
    expect(localStorage.getItem('whoami:boot:muted')).toBe('false')
    wrapper.unmount()
  })

  it('完整播放（降级路径）：逐行打印 ≥5 行日志后结束，总时长 ≤ 2.5s', async () => {
    stubMatchMedia(false)
    vi.useFakeTimers()
    const wrapper = mountBoot(true)

    // 站点配置等待上限 400ms
    await vi.advanceTimersByTimeAsync(400)
    // 打字序列 + 降级渐隐
    await vi.advanceTimersByTimeAsync(2100)

    expect(wrapper.findAll('.boot-line').length).toBeGreaterThanOrEqual(5)
    expect(wrapper.emitted('finished')).toHaveLength(1)
    expect(localStorage.getItem(BOOT_SEEN_KEY)).not.toBeNull()
    wrapper.unmount()
  })

  it('prefers-reduced-motion：静态版一次性呈现全部日志并快速结束', async () => {
    stubMatchMedia(true)
    vi.useFakeTimers()
    const wrapper = mountBoot(true)

    await vi.advanceTimersByTimeAsync(400)
    // 静态版：无打字过程，全部日志立即可见
    expect(wrapper.findAll('.boot-line').length).toBeGreaterThanOrEqual(5)

    await vi.advanceTimersByTimeAsync(900)
    expect(wrapper.emitted('finished')).toHaveLength(1)
    wrapper.unmount()
  })
})
