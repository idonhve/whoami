import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

import RepoCard from './RepoCard.vue'
import type { WorkCard } from '@/api/works'

vi.mock('@/api/track', () => ({
  trackGithubOutbound: vi.fn(),
}))

import { trackGithubOutbound } from '@/api/track'

const mockedTrack = vi.mocked(trackGithubOutbound)

function makeCard(overrides: Partial<WorkCard> = {}): WorkCard {
  return {
    id: 1,
    cnTitle: '我的个人主页',
    language: 'TypeScript',
    stargazersCount: 12,
    forksCount: 3,
    htmlUrl: 'https://github.com/idonhve/whoami-site',
    pushedAt: '2026-08-25T09:00:00',
    isPinned: false,
    sortOrder: 0,
    ...overrides,
  }
}

describe('作品卡片 RepoCard', () => {
  beforeEach(() => {
    localStorage.clear()
    mockedTrack.mockReset()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('渲染仓库名/描述/语言/star/fork/更新时间', () => {
    const wrapper = mount(RepoCard, { props: { work: makeCard() } })

    expect(wrapper.find('.repo-name').text()).toBe('whoami-site')
    expect(wrapper.find('.card-desc').text()).toBe('我的个人主页')
    expect(wrapper.find('.meta-item.lang').text()).toContain('TypeScript')
    expect(wrapper.find('.repo-card').attributes('style')).toContain('--lang-dot: var(--lang-ts)')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.find('.meta-item.updated').text()).toMatch(/ago$/)
  })

  it('置顶作品带 PIN 标记，未置顶没有', () => {
    const pinned = mount(RepoCard, { props: { work: makeCard({ isPinned: true }) } })
    const plain = mount(RepoCard, { props: { work: makeCard() } })

    expect(pinned.find('.pin-tag').text()).toBe('[PIN]')
    expect(plain.find('.pin-tag').exists()).toBe(false)
  })

  it('点击新标签打开 GitHub 仓库页并计入 github_outbound 埋点', async () => {
    const wrapper = mount(RepoCard, { props: { work: makeCard() } })

    const link = wrapper.find('a.card-link')
    expect(link.attributes('href')).toBe('https://github.com/idonhve/whoami-site')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')

    await link.trigger('click')
    expect(mockedTrack).toHaveBeenCalledWith('whoami-site')
  })

  it('中文描述为空时显示占位而非空白', () => {
    const wrapper = mount(RepoCard, {
      props: { work: makeCard({ cnTitle: null }) },
    })

    expect(wrapper.find('.card-desc').text()).toBe('(no description)')
    expect(wrapper.find('.card-desc').classes()).toContain('empty')
  })

  it('无 IntersectionObserver 环境直接可见（防卡片永远隐藏）', () => {
    const observerBackup = (window as { IntersectionObserver?: unknown }).IntersectionObserver
    ;(window as { IntersectionObserver?: unknown }).IntersectionObserver = undefined

    const wrapper = mount(RepoCard, { props: { work: makeCard() } })

    expect(wrapper.find('.repo-card').classes()).toContain('in-view')
    ;(window as { IntersectionObserver?: unknown }).IntersectionObserver = observerBackup
  })
})
