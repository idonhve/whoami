import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  deleteMessage,
  fetchAdminMessages,
  fetchPublicMessages,
  replyMessage,
  submitMessage,
  updateMessageStatus,
} from './message'

function okResponse(data: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => ({ code: 0, message: 'ok', data }),
  } as unknown as Response
}

const fetchMock = vi.fn()

afterEach(() => {
  vi.unstubAllGlobals()
  fetchMock.mockReset()
})

describe('公开留言接口（仅 approved，不含 email/IP 契约由后端保证）', () => {
  it('fetchPublicMessages() 调 GET /api/messages?limit=20', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(
      okResponse([{ id: 1, nickname: '访客', content: '赞', reply: null, repliedAt: null, createdAt: '2026-09-01T10:00:00' }]),
    )

    const data = await fetchPublicMessages()

    expect(fetchMock.mock.calls[0][0]).toBe('/api/messages?limit=20')
    expect(data[0].nickname).toBe('访客')
  })

  it('fetchPublicMessages(limit) 自定义条数', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([]))

    await fetchPublicMessages(5)

    expect(fetchMock.mock.calls[0][0]).toBe('/api/messages?limit=5')
  })

  it('submitMessage 调 POST /api/messages，payload 三字段', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse(null))

    await submitMessage({ nickname: 'HR', content: '约面', email: 'hr@x.com' })

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/messages')
    expect(init.method).toBe('POST')
    expect(JSON.parse(init.body)).toEqual({ nickname: 'HR', content: '约面', email: 'hr@x.com' })
  })
})

describe('后台留言管理接口（JWT 由 http 自动附带）', () => {
  it('fetchAdminMessages() 无筛选时调 GET /admin/api/messages（全量含状态）', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([{ id: 1, status: 'approved', ip: '1.2.*.*' }]))

    const data = await fetchAdminMessages()

    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/messages')
    expect(data[0].status).toBe('approved')
  })

  it('fetchAdminMessages("hidden") 带 status query', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([]))

    await fetchAdminMessages('hidden')

    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/messages?status=hidden')
  })

  it('replyMessage 调 PUT /admin/api/messages/{id}/reply', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse(null))

    await replyMessage(42, '感谢留言')

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/admin/api/messages/42/reply')
    expect(init.method).toBe('PUT')
    expect(JSON.parse(init.body)).toEqual({ reply: '感谢留言' })
  })

  it('updateMessageStatus 调 PUT /admin/api/messages/{id}/status（approved/hidden）', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse(null))

    await updateMessageStatus(7, 'hidden')

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/admin/api/messages/7/status')
    expect(init.method).toBe('PUT')
    expect(JSON.parse(init.body)).toEqual({ status: 'hidden' })
  })

  it('deleteMessage 调 DELETE /admin/api/messages/{id}', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse(null))

    await deleteMessage(9)

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/admin/api/messages/9')
    expect(init.method).toBe('DELETE')
  })
})
