import { mount } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { Certificate } from '@/api/certificate'
import Lightbox from './Lightbox.vue'

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

const ITEMS: Certificate[] = [
  makeCert(1, { name: 'AWS SAA' }),
  makeCert(2, { name: 'PMP' }),
  makeCert(3, { name: 'CKA' }),
]

function keydown(key: string) {
  window.dispatchEvent(new KeyboardEvent('keydown', { key }))
}

function emittedActive(wrapper: VueWrapper): unknown[] {
  return (wrapper.emitted('update:active') ?? []).map((args) => args[0])
}

describe('证书灯箱 Lightbox', () => {
  afterEach(() => {
    document.body.classList.remove('lb-scroll-lock')
    vi.restoreAllMocks()
  })

  it('关闭态（active=null）不渲染遮罩', () => {
    const wrapper = mount(Lightbox, { props: { items: ITEMS, active: null } })

    expect(wrapper.find('.lb-backdrop').exists()).toBe(false)
    expect(document.body.classList.contains('lb-scroll-lock')).toBe(false)
  })

  it('打开时才加载当前原图，并展示名称/获取时间/序号', () => {
    const wrapper = mount(Lightbox, { props: { items: ITEMS, active: 0 } })

    const image = wrapper.find('.lb-image')
    expect(image.exists()).toBe(true)
    expect(image.attributes('src')).toBe(ITEMS[0]!.imageUrl)
    expect(image.attributes('alt')).toBe('AWS SAA')
    expect(wrapper.find('.lb-name').text()).toBe('AWS SAA')
    expect(wrapper.find('.lb-date').text()).toBe('2024-06-01')
    expect(wrapper.find('.lb-count').text()).toBe('1 / 3')
    // 打开时锁定背景滚动
    expect(document.body.classList.contains('lb-scroll-lock')).toBe(true)
  })

  it('点击上一张/下一张切换（列表内环游）', async () => {
    const wrapper = mount(Lightbox, { props: { items: ITEMS, active: 0 } })

    await wrapper.find('.lb-next').trigger('click')
    expect(emittedActive(wrapper)).toEqual([1])
    expect(wrapper.find('.lb-image').attributes('src')).toBe(ITEMS[1]!.imageUrl)

    await wrapper.find('.lb-prev').trigger('click')
    await wrapper.find('.lb-prev').trigger('click')
    // 0 → 1 → 0 → 环游到最后一张
    expect(emittedActive(wrapper)).toEqual([1, 0, 2])
    expect(wrapper.find('.lb-image').attributes('src')).toBe(ITEMS[2]!.imageUrl)
  })

  it('键盘 ←/→ 切换、ESC 关闭', async () => {
    const wrapper = mount(Lightbox, { props: { items: ITEMS, active: 0 } })

    keydown('ArrowRight')
    keydown('ArrowRight')
    await Promise.resolve()
    expect(emittedActive(wrapper)).toEqual([1, 2])

    keydown('ArrowLeft')
    await Promise.resolve()
    expect(emittedActive(wrapper)).toEqual([1, 2, 1])

    keydown('Escape')
    await Promise.resolve()
    expect(emittedActive(wrapper)[emittedActive(wrapper).length - 1]).toBeNull()
  })

  it('点击遮罩关闭；点击图片本体不关闭', async () => {
    const wrapper = mount(Lightbox, { props: { items: ITEMS, active: 0 } })

    await wrapper.find('.lb-image').trigger('click')
    expect(wrapper.emitted('update:active')).toBeUndefined()

    await wrapper.find('.lb-backdrop').trigger('click')
    expect(emittedActive(wrapper)).toEqual([null])
  })

  it('关闭按钮发出关闭事件', async () => {
    const wrapper = mount(Lightbox, { props: { items: ITEMS, active: 1 } })

    await wrapper.find('.lb-close').trigger('click')
    expect(emittedActive(wrapper)).toEqual([null])
  })

  it('卸载时移除键盘监听与滚动锁', () => {
    const spy = vi.spyOn(window, 'removeEventListener')
    const wrapper = mount(Lightbox, { props: { items: ITEMS, active: 0 } })
    expect(document.body.classList.contains('lb-scroll-lock')).toBe(true)

    wrapper.unmount()
    expect(document.body.classList.contains('lb-scroll-lock')).toBe(false)
    expect(spy).toHaveBeenCalledWith('keydown', expect.any(Function))
    spy.mockRestore()
  })
})
