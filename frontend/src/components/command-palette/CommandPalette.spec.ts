import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import CommandPalette from '@/components/command-palette/CommandPalette.vue'
import { closePalette, openPalette, paletteOpen } from '@/components/command-palette/paletteOpen'
import { HISTORY_KEY } from '@/components/command-palette/history'
import { getLatest } from '@/api/resume'
import { fetchSiteConfig } from '@/api/siteConfig'
import { trackEvent } from '@/tracker'

vi.mock('@/api/resume', () => ({
  getLatest: vi.fn(),
  RESUME_DOWNLOAD_URL: '/api/resume/download',
}))

vi.mock('@/api/siteConfig', () => ({
  fetchSiteConfig: vi.fn(),
}))

vi.mock('@/tracker', () => ({
  trackEvent: vi.fn(),
}))

const getLatestMock = vi.mocked(getLatest)
const fetchSiteConfigMock = vi.mocked(fetchSiteConfig)
const trackEventMock = vi.mocked(trackEvent)

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>home</div>' } },
      { path: '/works', component: { template: '<div>works</div>' } },
      { path: '/tech', component: { template: '<div>tech</div>' } },
      { path: '/experience', component: { template: '<div>exp</div>' } },
      { path: '/awards', component: { template: '<div>awards</div>' } },
      { path: '/about', component: { template: '<div id="about">about</div>' } },
    ],
  })
}

async function mountPalette(): Promise<{ wrapper: ReturnType<typeof mount>; router: Router }> {
  const router = makeRouter()
  // attachTo 真实 DOM：focus/activeElement 断言需要（jsdom 对游离元素 focus 无效）
  activeHost = document.createElement('div')
  document.body.appendChild(activeHost)
  activeWrapper = mount(CommandPalette, { attachTo: activeHost, global: { plugins: [router] } })
  await router.isReady()
  return { wrapper: activeWrapper, router }
}

/** 每个用例独占的挂载引用：afterEach 卸载，避免全局 keydown 监听器跨用例累积 */
let activeWrapper: ReturnType<typeof mount> | null = null
let activeHost: HTMLDivElement | null = null

function pressGlobalCtrlK(): void {
  window.dispatchEvent(
    new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true, cancelable: true }),
  )
}

describe('命令面板 CommandPalette（Spec 10）', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    localStorage.clear()
    paletteOpen.value = false
    getLatestMock.mockReset()
    fetchSiteConfigMock.mockReset()
    trackEventMock.mockReset()
    document.documentElement.removeAttribute('data-cmd-theme')
  })

  afterEach(() => {
    activeWrapper?.unmount()
    activeHost?.remove()
    activeWrapper = null
    activeHost = null
    vi.useRealTimers()
    closePalette()
    vi.restoreAllMocks()
  })

  it('Ctrl+K 唤起面板，输入框自动聚焦；再按 Ctrl+K 关闭', async () => {
    const { wrapper } = await mountPalette()
    pressGlobalCtrlK()
    await vi.dynamicImportSettled()
    expect(paletteOpen.value).toBe(true)
    expect(wrapper.find('.palette-panel').exists()).toBe(true)
    expect(wrapper.find('input.palette-input').element).toBe(document.activeElement)

    pressGlobalCtrlK()
    expect(paletteOpen.value).toBe(false)
  })

  it('Cmd+K（Mac）同样唤起', async () => {
    await mountPalette()
    window.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'k', metaKey: true, bubbles: true, cancelable: true }),
    )
    expect(paletteOpen.value).toBe(true)
  })

  it('Esc 关闭面板', async () => {
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    await wrapper.find('input.palette-input').trigger('keydown', { key: 'Escape' })
    expect(paletteOpen.value).toBe(false)
  })

  it('页面输入框聚焦时 Ctrl+K 仍唤起（capture 拦截）', async () => {
    await mountPalette()
    const pageInput = document.createElement('input')
    document.body.appendChild(pageInput)
    pageInput.focus()
    pageInput.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true, cancelable: true }),
    )
    expect(paletteOpen.value).toBe(true)
    pageInput.remove()
  })

  it('模糊匹配 wrk → works，Enter 导航并回显、计入埋点、延迟后关闭', async () => {
    const { wrapper, router } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('wrk')
    const items = wrapper.findAll('.palette-item')
    expect(items.length).toBe(1)
    expect(items[0]?.text()).toContain('works')

    await input.trigger('keydown', { key: 'Enter' })
    // 回显可见期（420ms 关闭延迟内）：先 flush 微任务断言回显与导航
    await vi.advanceTimersByTimeAsync(0)
    expect(router.currentRoute.value.path).toBe('/works')
    expect(trackEventMock).toHaveBeenCalledWith('cmd_palette_use', { command: 'works' })
    expect(wrapper.find('.palette-echo-line').text()).toBe('> navigating to works ...')

    await vi.runAllTimersAsync()
    expect(paletteOpen.value).toBe(false)
  })

  it('↑/↓ 移动选中项，Enter 执行选中而非首个', async () => {
    const { wrapper, router } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    // 空输入：历史(空) + 全量命令，activeIndex 从 0 起
    await input.trigger('keydown', { key: 'ArrowDown' })
    await input.trigger('keydown', { key: 'ArrowDown' })
    const actives = wrapper.findAll('.palette-item.is-active')
    expect(actives.length).toBe(1)
    expect(actives[0]?.text()).toContain('tech')

    await input.trigger('keydown', { key: 'ArrowUp' })
    expect(wrapper.findAll('.palette-item.is-active')[0]?.text()).toContain('works')

    await input.trigger('keydown', { key: 'Enter' })
    await vi.runAllTimersAsync()
    expect(router.currentRoute.value.path).toBe('/works')
  })

  it('未知命令返回终端风 command not found，面板保留', async () => {
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('zzz')
    expect(wrapper.find('.palette-empty').text()).toBe('command not found: zzz')

    await input.trigger('keydown', { key: 'Enter' })
    const echo = wrapper.findAll('.palette-echo-line')
    expect(echo[echo.length - 1]?.text()).toBe('command not found: zzz')
    expect(echo[echo.length - 1]?.classes()).toContain('error')
    expect(paletteOpen.value).toBe(true)
  })

  it('help 列出全部命令：输入清空、面板保留', async () => {
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('help')
    await input.trigger('keydown', { key: 'Enter' })
    await vi.runAllTimersAsync()

    expect((input.element as HTMLInputElement).value).toBe('')
    const names = wrapper.findAll('.item-name').map((n) => n.text())
    for (const expected of ['home', 'works', 'tech', 'experience', 'awards', 'resume', 'message', 'github', 'theme', 'help']) {
      expect(names).toContain(expected)
    }
    expect(paletteOpen.value).toBe(true)
    expect(trackEventMock).toHaveBeenCalledWith('cmd_palette_use', { command: 'help' })
  })

  it('历史记录：执行过的命令在下次打开时出现在 history 分区', async () => {
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('tech')
    await input.trigger('keydown', { key: 'Enter' })
    await vi.runAllTimersAsync()

    expect(JSON.parse(localStorage.getItem(HISTORY_KEY) ?? '[]')).toEqual(['tech'])

    openPalette()
    await vi.dynamicImportSettled()
    expect(wrapper.find('.palette-section').text()).toBe('history')
    expect(wrapper.find('.palette-item.is-history .item-name').text()).toBe('tech')
  })

  it('resume 命令：有版本时触发下载锚点并回显', async () => {
    getLatestMock.mockResolvedValue({ exists: true, displayName: 'whoami_简历_2026-09.pdf', updatedAt: null })
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('resume')
    await input.trigger('keydown', { key: 'Enter' })
    await vi.advanceTimersByTimeAsync(0)

    expect(clickSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.palette-echo-line').text()).toContain('downloading whoami_简历_2026-09.pdf')

    await vi.runAllTimersAsync()
    expect(paletteOpen.value).toBe(false)
  })

  it('resume 命令：站主未上传 → command not found 提示', async () => {
    getLatestMock.mockResolvedValue({ exists: false, displayName: null, updatedAt: null })
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('resume')
    await input.trigger('keydown', { key: 'Enter' })
    await vi.runAllTimersAsync()

    // 失败回显保留面板（与未知命令行为一致），错误行不闪没
    expect(wrapper.find('.palette-echo-line.error').text()).toContain('command not found')
    expect(paletteOpen.value).toBe(true)
  })

  it('github 命令：读配置新标签打开并上报 github_outbound', async () => {
    fetchSiteConfigMock.mockResolvedValue({
      domain: 'localhost',
      ownerName: '站主',
      githubUrl: 'https://github.com/idonhve',
      degradeForceFull: false,
    })
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('github')
    await input.trigger('keydown', { key: 'Enter' })
    await vi.advanceTimersByTimeAsync(0)

    expect(openSpy).toHaveBeenCalledWith('https://github.com/idonhve', '_blank', 'noopener,noreferrer')
    expect(trackEventMock).toHaveBeenCalledWith('github_outbound', { source: 'palette' })
    expect(wrapper.find('.palette-echo-line').text()).toBe('> opening github ...')

    await vi.runAllTimersAsync()
    expect(paletteOpen.value).toBe(false)
  })

  it('github 命令：未配置 githubUrl → command not found', async () => {
    fetchSiteConfigMock.mockResolvedValue({
      domain: 'localhost',
      ownerName: '站主',
      githubUrl: '',
      degradeForceFull: false,
    })
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('github')
    await input.trigger('keydown', { key: 'Enter' })
    await vi.runAllTimersAsync()

    expect(wrapper.find('.palette-echo-line.error').text()).toContain('command not found')
    expect(paletteOpen.value).toBe(true)
  })

  it('theme 命令：循环切换强调色并持久化，面板保留实时可见', async () => {
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    const input = wrapper.find('input.palette-input')
    await input.setValue('theme')
    await input.trigger('keydown', { key: 'Enter' })
    await vi.runAllTimersAsync()

    expect(document.documentElement.getAttribute('data-cmd-theme')).toBe('cyan')
    expect(localStorage.getItem('whoami:theme')).toBe('cyan')
    expect(wrapper.find('.palette-echo-line').text()).toBe('> switching theme to cyan ...')
    expect(paletteOpen.value).toBe(true)
  })

  it('点击遮罩关闭面板', async () => {
    const { wrapper } = await mountPalette()
    openPalette()
    await vi.dynamicImportSettled()
    await wrapper.find('.palette-backdrop').trigger('click')
    expect(paletteOpen.value).toBe(false)
  })
})
