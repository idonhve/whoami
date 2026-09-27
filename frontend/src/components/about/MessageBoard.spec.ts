import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import MessageBoard from './MessageBoard.vue'
import { ApiError } from '@/api/http'
import { fetchPublicMessages, submitMessage, type MessageItem } from '@/api/message'
import { trackEvent } from '@/tracker'

vi.mock('@/api/message', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/message')>()
  return {
    ...actual,
    fetchPublicMessages: vi.fn(),
    submitMessage: vi.fn(),
  }
})

vi.mock('@/tracker', () => ({
  trackEvent: vi.fn(),
}))

const mockedFetchList = vi.mocked(fetchPublicMessages)
const mockedSubmit = vi.mocked(submitMessage)
const mockedTrackEvent = vi.mocked(trackEvent)

function msgFixture(partial: Partial<MessageItem>): MessageItem {
  return {
    id: 1,
    nickname: 'HR小李',
    content: '简历看过了，随时联系。',
    reply: null,
    repliedAt: null,
    createdAt: '2026-09-01T10:30:00',
    ...partial,
  }
}

/** toast 经 Teleport 挂到 body */
function bodyToast(): string {
  return document.body.querySelector('.toast')?.textContent ?? ''
}

async function mountBoard() {
  const wrapper = mount(MessageBoard)
  await flushPromises()
  return wrapper
}

async function submitForm(wrapper: ReturnType<typeof mount>) {
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

beforeEach(() => {
  vi.clearAllMocks()
  document.body.innerHTML = ''
  mockedFetchList.mockResolvedValue([])
})

describe('留言列表（GET /api/messages?limit=20）', () => {
  it('加载列表：昵称 / 内容 / 站主回复 / 时间', async () => {
    mockedFetchList.mockResolvedValue([
      msgFixture({
        id: 7,
        nickname: '面试官',
        content: '项目经验不错',
        reply: '感谢，欢迎深聊',
        createdAt: '2026-09-12T08:00:00',
      }),
    ])
    const wrapper = await mountBoard()

    expect(mockedFetchList).toHaveBeenCalledWith(20)
    expect(wrapper.text()).toContain('面试官')
    expect(wrapper.text()).toContain('项目经验不错')
    expect(wrapper.text()).toContain('> 站主回复')
    expect(wrapper.text()).toContain('感谢，欢迎深聊')
    expect(wrapper.text()).toContain('2026-09-12 08:00')
  })

  it('加载失败显示离线提示且不渲染表单数据', async () => {
    mockedFetchList.mockRejectedValue(new Error('network down'))
    const wrapper = await mountBoard()
    expect(wrapper.text()).toContain('留言板暂时离线')
  })
})

describe('前端预校验（后端 400 双保险的前一道闸）', () => {
  it('昵称为空提交：字段错误提示且不调提交接口', async () => {
    const wrapper = await mountBoard()
    await wrapper.find('textarea[name="content"]').setValue('你好')
    await submitForm(wrapper)

    expect(wrapper.text()).toContain('nickname 必填')
    expect(mockedSubmit).not.toHaveBeenCalled()
  })

  it('内容为空提交：字段错误提示且不调提交接口', async () => {
    const wrapper = await mountBoard()
    await wrapper.find('input[name="nickname"]').setValue('访客')
    await submitForm(wrapper)

    expect(wrapper.text()).toContain('content 必填')
    expect(mockedSubmit).not.toHaveBeenCalled()
  })

  it('邮箱格式不正确：字段错误提示且不调提交接口', async () => {
    const wrapper = await mountBoard()
    await wrapper.find('input[name="nickname"]').setValue('访客')
    await wrapper.find('input[name="email"]').setValue('not-an-email')
    await wrapper.find('textarea[name="content"]').setValue('你好')
    await submitForm(wrapper)

    expect(wrapper.text()).toContain('email 格式不正确')
    expect(mockedSubmit).not.toHaveBeenCalled()
  })
})

describe('提交成功', () => {
  it('toast 显示 `> message sent ✓`，打 message_submit 埋点并刷新列表', async () => {
    mockedFetchList
      .mockResolvedValueOnce([]) // 初始加载
      .mockResolvedValueOnce([msgFixture({ id: 9 })]) // 提交后刷新
    const wrapper = await mountBoard()

    await wrapper.find('input[name="nickname"]').setValue('HR')
    await wrapper.find('input[name="email"]').setValue('hr@example.com')
    await wrapper.find('textarea[name="content"]').setValue('想约面试')
    await submitForm(wrapper)

    expect(mockedSubmit).toHaveBeenCalledWith({
      nickname: 'HR',
      content: '想约面试',
      email: 'hr@example.com',
    })
    expect(mockedTrackEvent).toHaveBeenCalledWith('message_submit')
    expect(bodyToast()).toBe('> message sent ✓')
    // 表单清空
    expect((wrapper.find('textarea[name="content"]').element as HTMLTextAreaElement).value).toBe('')
  })

  it('邮箱为空时不携带 email 字段', async () => {
    const wrapper = await mountBoard()
    await wrapper.find('input[name="nickname"]').setValue('访客')
    await wrapper.find('textarea[name="content"]').setValue('赞')
    await submitForm(wrapper)

    expect(mockedSubmit).toHaveBeenCalledWith({ nickname: '访客', content: '赞' })
  })
})

describe('提交失败', () => {
  it('429 限流：toast 显示限流提示', async () => {
    mockedSubmit.mockRejectedValue(new ApiError(429, 429, '请求过于频繁'))
    const wrapper = await mountBoard()
    await wrapper.find('input[name="nickname"]').setValue('访客')
    await wrapper.find('textarea[name="content"]').setValue('灌水')
    await submitForm(wrapper)

    expect(bodyToast()).toContain('429')
    expect(bodyToast()).toContain('每分钟最多 3 条')
  })

  it('后端 400：toast 透出后端校验 message', async () => {
    mockedSubmit.mockRejectedValue(new ApiError(400, 400, 'nickname 不能超过 20 字符'))
    const wrapper = await mountBoard()
    await wrapper.find('input[name="nickname"]').setValue('访客')
    await wrapper.find('textarea[name="content"]').setValue('你好')
    await submitForm(wrapper)

    expect(bodyToast()).toContain('nickname 不能超过 20 字符')
  })

  it('未知错误：兜底错误提示', async () => {
    mockedSubmit.mockRejectedValue(new Error('boom'))
    const wrapper = await mountBoard()
    await wrapper.find('input[name="nickname"]').setValue('访客')
    await wrapper.find('textarea[name="content"]').setValue('你好')
    await submitForm(wrapper)

    expect(bodyToast()).toContain('提交失败')
  })
})
