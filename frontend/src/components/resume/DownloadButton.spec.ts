import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import DownloadButton from './DownloadButton.vue'
import { getLatest } from '@/api/resume'

vi.mock('@/api/resume', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/resume')>()
  return {
    ...actual,
    getLatest: vi.fn(),
  }
})

const mockedGetLatest = vi.mocked(getLatest)

describe('简历下载按钮（Spec 07 唯一实现）', () => {
  beforeEach(() => {
    mockedGetLatest.mockReset()
  })

  it('exists=false 时整体隐藏且不报错', async () => {
    mockedGetLatest.mockResolvedValue({ exists: false, displayName: null, updatedAt: null })
    const wrapper = mount(DownloadButton)
    await flushPromises()

    expect(wrapper.find('.resume-download').exists()).toBe(false)
    expect(wrapper.text()).toBe('')
  })

  it('接口异常时同样隐藏（静默降级）', async () => {
    mockedGetLatest.mockRejectedValue(new Error('network down'))
    const wrapper = mount(DownloadButton)
    await flushPromises()

    expect(wrapper.find('.resume-download').exists()).toBe(false)
  })

  it('exists=true 时渲染终端风命令，文案随 displayName 动态', async () => {
    mockedGetLatest.mockResolvedValue({
      exists: true,
      displayName: '张三_简历_2026-08.pdf',
      updatedAt: '2026-08-20T10:30:00',
    })
    const wrapper = mount(DownloadButton)
    await flushPromises()

    expect(wrapper.find('.cmd-text').text()).toBe('download 张三_简历_2026-08.pdf')
    expect(wrapper.find('.prompt').text()).toBe('>')
    expect(wrapper.find('.meta').text()).toContain('2026-08-20')
  })

  it('点击即经 /api/resume/download 导航下载（无跳转页无二次确认）', async () => {
    mockedGetLatest.mockResolvedValue({
      exists: true,
      displayName: '张三_简历_2026-08.pdf',
      updatedAt: '2026-08-20T10:30:00',
    })
    const wrapper = mount(DownloadButton)
    await flushPromises()

    const anchor = wrapper.find('a.resume-download')
    expect(anchor.exists()).toBe(true)
    expect(anchor.attributes('href')).toBe('/api/resume/download')
    expect(anchor.attributes('download')).toBe('张三_简历_2026-08.pdf')
  })

  it('displayName 为空时回退 resume.pdf 文案', async () => {
    mockedGetLatest.mockResolvedValue({ exists: true, displayName: null, updatedAt: null })
    const wrapper = mount(DownloadButton)
    await flushPromises()

    expect(wrapper.find('.cmd-text').text()).toBe('download resume.pdf')
  })
})
