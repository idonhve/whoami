import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import MessagesAdminView from './MessagesAdminView.vue'
import {
  deleteMessage,
  fetchAdminMessages,
  replyMessage,
  updateMessageStatus,
  type AdminMessageItem,
} from '@/api/message'

vi.mock('@/api/message', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/message')>()
  return {
    ...actual,
    fetchAdminMessages: vi.fn(),
    replyMessage: vi.fn(),
    updateMessageStatus: vi.fn(),
    deleteMessage: vi.fn(),
  }
})

const mockedFetch = vi.mocked(fetchAdminMessages)
const mockedReply = vi.mocked(replyMessage)
const mockedStatus = vi.mocked(updateMessageStatus)
const mockedDelete = vi.mocked(deleteMessage)

function itemFixture(partial: Partial<AdminMessageItem>): AdminMessageItem {
  return {
    id: 1,
    nickname: 'HR小李',
    email: 'hr@x.com',
    content: '想约面试，请回复',
    status: 'approved',
    reply: null,
    repliedAt: null,
    ip: '1.2.*.*',
    createdAt: '2026-09-20T10:30:00',
    ...partial,
  }
}

async function mountView() {
  const wrapper = mount(MessagesAdminView)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedFetch.mockResolvedValue([])
})

describe('留言列表与筛选', () => {
  it('默认加载全量列表，展示昵称 / 脱敏 IP / 状态', async () => {
    mockedFetch.mockResolvedValue([
      itemFixture({}),
      itemFixture({ id: 2, nickname: '匿名', email: null, status: 'hidden', ip: '10.0.*.*' }),
    ])
    const wrapper = await mountView()

    expect(mockedFetch).toHaveBeenCalledWith(undefined)
    expect(wrapper.text()).toContain('HR小李')
    expect(wrapper.text()).toContain('1.2.*.*')
    expect(wrapper.text()).toContain('hidden')
    expect(wrapper.text()).toContain('approved')
  })

  it('点击「已下架」筛选带 status=hidden 重新加载', async () => {
    const wrapper = await mountView()
    mockedFetch.mockClear()
    await wrapper.findAll('.filter-btn')[2].trigger('click')
    await flushPromises()

    expect(mockedFetch).toHaveBeenCalledWith('hidden')
  })
})

describe('回复（PUT reply）', () => {
  it('点回复展开行内编辑，保存后调 replyMessage 并刷新', async () => {
    mockedFetch
      .mockResolvedValueOnce([itemFixture({ id: 5 })])
      .mockResolvedValueOnce([itemFixture({ id: 5, reply: '好的，邮件已发' })])
    const wrapper = await mountView()

    await wrapper.findAll('.op-btn')[0].trigger('click') // 回复
    await flushPromises()
    const textarea = wrapper.find('textarea.reply-input')
    expect(textarea.exists()).toBe(true)

    await textarea.setValue('好的，邮件已发')
    await wrapper.find('.reply-actions .op-btn.primary').trigger('click')
    await flushPromises()

    expect(mockedReply).toHaveBeenCalledWith(5, '好的，邮件已发')
  })
})

describe('下架 / 恢复（PUT status）', () => {
  it('approved 留言点「下架」调 updateMessageStatus(id, "hidden")', async () => {
    mockedFetch.mockResolvedValue([itemFixture({ id: 3, status: 'approved' })])
    const wrapper = await mountView()

    // 行内操作按钮：回复 / 下架 / 删除
    const ops = wrapper.findAll('tbody tr')[0].findAll('.op-btn')
    await ops[1].trigger('click')
    await flushPromises()

    expect(mockedStatus).toHaveBeenCalledWith(3, 'hidden')
  })

  it('hidden 留言点「恢复」调 updateMessageStatus(id, "approved")', async () => {
    mockedFetch.mockResolvedValue([itemFixture({ id: 4, status: 'hidden' })])
    const wrapper = await mountView()

    const ops = wrapper.findAll('tbody tr')[0].findAll('.op-btn')
    await ops[1].trigger('click')
    await flushPromises()

    expect(mockedStatus).toHaveBeenCalledWith(4, 'approved')
  })
})

describe('删除（确认弹窗）', () => {
  it('点删除先弹确认，确认后才调 DELETE', async () => {
    mockedFetch.mockResolvedValue([itemFixture({ id: 8 })])
    const wrapper = await mountView()

    const ops = wrapper.findAll('tbody tr')[0].findAll('.op-btn')
    await ops[2].trigger('click') // 删除
    await flushPromises()

    // 弹窗已出，尚未删除
    const modal = document.body.querySelector('.modal')
    expect(modal).not.toBeNull()
    expect(mockedDelete).not.toHaveBeenCalled()

    await (document.body.querySelector('.modal-actions .op-btn.danger') as HTMLButtonElement).click()
    await flushPromises()

    expect(mockedDelete).toHaveBeenCalledWith(8)
  })

  it('取消删除不调接口', async () => {
    mockedFetch.mockResolvedValue([itemFixture({ id: 8 })])
    const wrapper = await mountView()

    await wrapper.findAll('tbody tr')[0].findAll('.op-btn')[2].trigger('click')
    await flushPromises()
    await (document.body.querySelector('.modal-actions .op-btn:not(.danger)') as HTMLButtonElement).click()
    await flushPromises()

    expect(mockedDelete).not.toHaveBeenCalled()
  })
})

describe('空数据兜底', () => {
  it('无留言时显示占位行', async () => {
    const wrapper = await mountView()
    expect(wrapper.text()).toContain('暂无留言')
  })
})
