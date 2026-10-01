import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import GithubIcon from '@/components/shared/GithubIcon.vue'
import { fetchSiteConfig } from '@/api/siteConfig'
import { trackGithubIconClick } from '@/api/track'

vi.mock('@/api/siteConfig', () => ({
  fetchSiteConfig: vi.fn(),
}))

vi.mock('@/api/track', () => ({
  trackGithubIconClick: vi.fn(),
}))

const fetchSiteConfigMock = vi.mocked(fetchSiteConfig)
const trackGithubIconClickMock = vi.mocked(trackGithubIconClick)

const FULL_CONFIG = {
  domain: 'localhost',
  ownerName: '站主',
  githubUrl: '',
  degradeForceFull: false,
}

/** WCAG 相对亮度（global.css :root 的 --text/#04070b 背景，图形类标准 ≥ 3:1） */
function channel(c: number): number {
  const s = c / 255
  return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4)
}

function luminance(hex: string): number {
  const n = parseInt(hex.slice(1), 16)
  return (
    0.2126 * channel((n >> 16) & 0xff) +
    0.7152 * channel((n >> 8) & 0xff) +
    0.0722 * channel(n & 0xff)
  )
}

function contrastRatio(a: string, b: string): number {
  const [l1, l2] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (l1 + 0.05) / (l2 + 0.05)
}

async function mountWith(githubUrl: string, source: 'header' | 'footer' = 'header') {
  fetchSiteConfigMock.mockResolvedValue({ ...FULL_CONFIG, githubUrl })
  const wrapper = mount(GithubIcon, { props: { source } })
  await flushPromises()
  return wrapper
}

describe('GithubIcon 组件（Spec 03）', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('githubUrl 为空 → 整体不渲染', async () => {
    const wrapper = await mountWith('')
    expect(wrapper.find('a').exists()).toBe(false)
    expect(wrapper.text()).toBe('')
  })

  it('githubUrl 非空 → 渲染且 href/target/rel/aria-label 正确', async () => {
    const wrapper = await mountWith('https://github.com/idonhve')
    const link = wrapper.find('a.github-link')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('https://github.com/idonhve')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
    expect(link.attributes('aria-label')).toBe('GitHub 主页')
    expect(link.find('svg[aria-hidden="true"]').exists()).toBe(true)
  })

  it('点击触发 github_outbound 埋点，detail 带 header 来源', async () => {
    const wrapper = await mountWith('https://github.com/idonhve', 'header')
    await wrapper.find('a.github-link').trigger('click')
    expect(trackGithubIconClickMock).toHaveBeenCalledTimes(1)
    expect(trackGithubIconClickMock).toHaveBeenCalledWith('header')
  })

  it('页脚挂载的图标点击上报 footer 来源', async () => {
    const wrapper = await mountWith('https://github.com/idonhve', 'footer')
    await wrapper.find('a.github-link').trigger('click')
    expect(trackGithubIconClickMock).toHaveBeenCalledWith('footer')
  })

  it('图标前景与暗色背景对比度 ≥ 3:1（图形类标准）', () => {
    // --text 与 hover 态 --green 分别对 --bg 断言（token 值来自 global.css :root）
    expect(contrastRatio('#c8d6e5', '#04070b')).toBeGreaterThanOrEqual(3)
    expect(contrastRatio('#00ff9c', '#04070b')).toBeGreaterThanOrEqual(3)
  })
})
