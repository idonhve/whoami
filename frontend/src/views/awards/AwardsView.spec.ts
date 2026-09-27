import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { Certificate } from '@/api/certificate'

const mocks = vi.hoisted(() => ({
  fetchCertificates: vi.fn(),
}))

vi.mock('@/api/certificate', () => ({
  fetchCertificates: mocks.fetchCertificates,
}))

import AwardsView from './AwardsView.vue'

const mockedFetch = vi.mocked(mocks.fetchCertificates)

const FRONT_LAYOUT_STUB = { template: '<div><slot /></div>' }

function makeCert(id: number, overrides: Partial<Certificate> = {}): Certificate {
  return {
    id,
    name: `证书${id}`,
    obtainedAt: '2024-06-01',
    thumbUrl: `/uploads/certificate/${id}/thumb.webp`,
    imageUrl: `/uploads/certificate/${id}/image.webp`,
    sortOrder: id,
    ...overrides,
  }
}

const SAMPLE: Certificate[] = [
  makeCert(1, { name: 'AWS SAA' }),
  makeCert(2, { name: 'PMP' }),
  makeCert(3, { name: 'CKA' }),
]

/** 替身观察器：记录实例与目标，由测试手动触发回调 */
class FakeIntersectionObserver {
  static instances: FakeIntersectionObserver[] = []
  callback: (entries: IntersectionObserverEntry[], observer: IntersectionObserver) => void
  targets: Element[] = []
  constructor(callback: (entries: IntersectionObserverEntry[], observer: IntersectionObserver) => void) {
    this.callback = callback
    FakeIntersectionObserver.instances.push(this)
  }
  observe(target: Element): void {
    this.targets.push(target)
  }
  unobserve(): void {}
  disconnect(): void {}
}

function fireIntersection(io: FakeIntersectionObserver, target: Element) {
  io.callback(
    [{ isIntersecting: true, target } as IntersectionObserverEntry],
    io as unknown as IntersectionObserver,
  )
}

function mountView() {
  return mount(AwardsView, {
    global: { stubs: { FrontLayout: FRONT_LAYOUT_STUB } },
  })
}

describe('证书照片墙 AwardsView', () => {
  beforeEach(() => {
    mockedFetch.mockReset()
    FakeIntersectionObserver.instances.length = 0
    vi.stubGlobal('IntersectionObserver', FakeIntersectionObserver)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    document.body.classList.remove('lb-scroll-lock')
  })

  it('懒加载：初始不请求缩略图，进入视口前 500px 才开始加载', async () => {
    mockedFetch.mockResolvedValue(SAMPLE)
    const wrapper = mountView()
    await flushPromises()

    // 观察器已建立（先懒加载观察器，后入场观察器）
    expect(FakeIntersectionObserver.instances.length).toBe(2)
    expect(wrapper.findAll('.award-thumb')).toHaveLength(0)

    // 第一张进入预载区：只加载它的缩略图
    const cards = wrapper.findAll('.award-card')
    const lazyIo = FakeIntersectionObserver.instances[0]!
    fireIntersection(lazyIo, cards[0]!.element)
    await flushPromises()

    const thumbs = wrapper.findAll('.award-thumb')
    expect(thumbs).toHaveLength(1)
    expect(thumbs[0]!.attributes('src')).toBe(SAMPLE[0]!.thumbUrl)
    expect(thumbs[0]!.attributes('alt')).toBe('AWS SAA')
  })

  it('翻转入场：滚入视口才加 in-view，交错延迟按序号设置', async () => {
    mockedFetch.mockResolvedValue(SAMPLE)
    const wrapper = mountView()
    await flushPromises()

    const cards = wrapper.findAll('.award-card')
    expect(cards[0]!.classes()).not.toContain('in-view')
    // 交错延迟（交错入场）
    expect(cards[0]!.attributes('style')).toMatch(/--card-delay:\s*0ms/)
    expect(cards[2]!.attributes('style')).toMatch(/--card-delay:\s*140ms/)

    const enterIo = FakeIntersectionObserver.instances[1]!
    fireIntersection(enterIo, cards[0]!.element)
    await flushPromises()

    expect(cards[0]!.classes()).toContain('in-view')
    expect(cards[1]!.classes()).not.toContain('in-view')
  })

  it('hover 浮层展示证书名称与获取时间', async () => {
    mockedFetch.mockResolvedValue(SAMPLE)
    const wrapper = mountView()
    await flushPromises()

    const overlay = wrapper.findAll('.award-overlay')[0]!
    expect(overlay.text()).toContain('AWS SAA')
    expect(overlay.text()).toContain('2024-06-01')
  })

  it('点击卡片打开灯箱并按需加载压缩原图', async () => {
    mockedFetch.mockResolvedValue(SAMPLE)
    const wrapper = mountView()
    await flushPromises()

    // 灯箱关闭时没有原图请求
    expect(wrapper.find('.lb-image').exists()).toBe(false)

    await wrapper.findAll('.award-card')[1]!.trigger('click')
    await flushPromises()

    const image = wrapper.find('.lb-image')
    expect(image.exists()).toBe(true)
    expect(image.attributes('src')).toBe(SAMPLE[1]!.imageUrl)
    expect(wrapper.find('.lb-count').text()).toBe('2 / 3')
  })

  it('无 IntersectionObserver 环境直接全部显示（防内容永远空白）', async () => {
    vi.unstubAllGlobals()
    const observerBackup = (window as { IntersectionObserver?: unknown }).IntersectionObserver
    ;(window as { IntersectionObserver?: unknown }).IntersectionObserver = undefined

    mockedFetch.mockResolvedValue(SAMPLE)
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.findAll('.award-thumb')).toHaveLength(3)
    expect(wrapper.findAll('.award-card.in-view')).toHaveLength(3)

    ;(window as { IntersectionObserver?: unknown }).IntersectionObserver = observerBackup
  })

  it('空数据显示占位提示', async () => {
    mockedFetch.mockResolvedValue([])
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.find('.empty-state').text()).toContain('$ ls ~/certificates')
    expect(wrapper.find('.empty-state').text()).toContain('证书照片墙尚未上传')
    expect(wrapper.find('.awards-grid').exists()).toBe(false)
  })

  it('加载失败显示 [error] 而非空白', async () => {
    mockedFetch.mockRejectedValue(new Error('boom'))
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.sub').text()).toContain('[error]')
    expect(wrapper.find('.awards-grid').exists()).toBe(false)
  })
})
