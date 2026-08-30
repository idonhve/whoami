import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

import FeaturedWorks from './FeaturedWorks.vue'
import { fetchWorks } from '@/api/works'
import type { WorkCard } from '@/api/works'

vi.mock('@/api/works', () => ({
  fetchWorks: vi.fn(),
}))
vi.mock('@/api/track', () => ({
  trackGithubOutbound: vi.fn(),
}))

const mockedFetchWorks = vi.mocked(fetchWorks)

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/works', name: 'works', component: { template: '<div />' } },
    ],
  })
}

const PINNED: WorkCard[] = [
  {
    id: 1,
    cnTitle: '精选一',
    language: 'Java',
    stargazersCount: 9,
    forksCount: 2,
    htmlUrl: 'https://github.com/idonhve/one',
    pushedAt: '2026-08-25T09:00:00',
    isPinned: true,
    sortOrder: 5,
  },
]

describe('首页精选作品区', () => {
  beforeEach(() => {
    localStorage.clear()
    mockedFetchWorks.mockReset()
  })

  it('仅请求置顶项（scope=featured）并复用 RepoCard 渲染', async () => {
    mockedFetchWorks.mockResolvedValue(PINNED)
    const router = makeRouter()

    const wrapper = mount(FeaturedWorks, { global: { plugins: [router] } })
    await flushPromises()

    expect(mockedFetchWorks).toHaveBeenCalledWith('featured')
    expect(wrapper.findAll('.repo-card')).toHaveLength(1)
    expect(wrapper.find('.slot-cmd').text()).toBe('$ ls ~/featured')
  })

  it('无置顶时展示引导语与 /works 入口', async () => {
    mockedFetchWorks.mockResolvedValue([])
    const router = makeRouter()

    const wrapper = mount(FeaturedWorks, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('.repo-card').exists()).toBe(false)
    expect(wrapper.find('.more-link').attributes('href')).toBe('/works')
  })

  it('接口失败静默降级为占位提示（不报错不空白）', async () => {
    mockedFetchWorks.mockRejectedValue(new Error('network down'))
    const router = makeRouter()

    const wrapper = mount(FeaturedWorks, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('.slot-line.dim').text()).toContain('精选作品暂不可用')
  })
})
