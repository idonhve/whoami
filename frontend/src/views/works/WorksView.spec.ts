import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import WorksView from './WorksView.vue'
import { fetchWorks } from '@/api/works'
import type { WorkCard } from '@/api/works'

vi.mock('@/api/works', () => ({
  fetchWorks: vi.fn(),
}))
vi.mock('@/api/track', () => ({
  trackGithubOutbound: vi.fn(),
}))

const mockedFetchWorks = vi.mocked(fetchWorks)

const CARDS: WorkCard[] = [
  {
    id: 1,
    cnTitle: '置顶项目',
    language: 'Java',
    stargazersCount: 9,
    forksCount: 2,
    htmlUrl: 'https://github.com/idonhve/pinned',
    pushedAt: '2026-08-25T09:00:00',
    isPinned: true,
    sortOrder: 5,
  },
  {
    id: 2,
    cnTitle: null,
    language: 'Go',
    stargazersCount: 1,
    forksCount: 0,
    htmlUrl: 'https://github.com/idonhve/plain',
    pushedAt: '2026-08-01T09:00:00',
    isPinned: false,
    sortOrder: 0,
  },
]

const FRONT_LAYOUT_STUB = { template: '<div><slot /></div>' }

describe('作品页 /works', () => {
  beforeEach(() => {
    localStorage.clear()
    mockedFetchWorks.mockReset()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('渲染卡片列表与置顶标记，scope=all', async () => {
    mockedFetchWorks.mockResolvedValue(CARDS)

    const wrapper = mount(WorksView, {
      global: { stubs: { FrontLayout: FRONT_LAYOUT_STUB } },
    })
    await flushPromises()

    expect(mockedFetchWorks).toHaveBeenCalledWith('all')
    expect(wrapper.findAll('.repo-card')).toHaveLength(2)
    expect(wrapper.find('.sub').text()).toContain('2 repos')
    const pinnedCard = wrapper.findAll('.repo-card')[0]
    expect(pinnedCard.find('.pin-tag').text()).toBe('[PIN]')
  })

  it('空列表展示等待同步空态', async () => {
    mockedFetchWorks.mockResolvedValue([])

    const wrapper = mount(WorksView, {
      global: { stubs: { FrontLayout: FRONT_LAYOUT_STUB } },
    })
    await flushPromises()

    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.find('.sub').text()).toContain('0 repos')
  })

  it('接口失败不白屏，展示错误提示', async () => {
    mockedFetchWorks.mockRejectedValue(new Error('network down'))

    const wrapper = mount(WorksView, {
      global: { stubs: { FrontLayout: FRONT_LAYOUT_STUB } },
    })
    await flushPromises()

    expect(wrapper.find('.repo-card').exists()).toBe(false)
    expect(wrapper.find('.sub').text()).toContain('[error]')
  })
})
