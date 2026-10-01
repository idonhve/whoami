import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { ApiError, http, TOKEN_KEY } from './http'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('http 客户端 401 续签链路', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('401 后自动续签成功：以新 token 重试原请求并返回数据', async () => {
    localStorage.setItem(TOKEN_KEY, 'expired-token')
    const calls: { url: string; auth?: string }[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const auth = (init?.headers as Record<string, string> | undefined)?.Authorization
        calls.push({ url, auth })
        if (url === '/admin/api/auth/refresh') {
          expect(auth).toBe('Bearer expired-token')
          return jsonResponse(200, { code: 0, message: 'ok', data: { token: 'fresh-token', expiresIn: 7200 } })
        }
        if (auth === 'Bearer expired-token') {
          return jsonResponse(401, { code: 401, message: 'token 无效或已过期', data: null })
        }
        return jsonResponse(200, { code: 0, message: 'ok', data: 'payload' })
      }),
    )

    const result = await http.get<string>('/admin/api/site-config')

    expect(result).toBe('payload')
    expect(localStorage.getItem(TOKEN_KEY)).toBe('fresh-token')
    // 原请求 2 次（旧 token 401 + 新 token 重试）+ 续签 1 次
    expect(calls.filter((c) => c.url === '/admin/api/site-config')).toHaveLength(2)
    expect(calls.filter((c) => c.url === '/admin/api/auth/refresh')).toHaveLength(1)
    expect(calls[2].auth).toBe('Bearer fresh-token')
  })

  it('续签也 401：清除 token 并抛出 401 ApiError', async () => {
    localStorage.setItem(TOKEN_KEY, 'expired-token')
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse(401, { code: 401, message: 'token 无效或已过期', data: null })),
    )

    await expect(http.get('/admin/api/site-config')).rejects.toMatchObject({
      status: 401,
    } satisfies Partial<ApiError>)
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
  })

  it('登录接口的 401 不触发续签（账号密码错误直接抛出）', async () => {
    const fetchMock = vi.fn(async () =>
      jsonResponse(401, { code: 401, message: '用户名或密码错误', data: null }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      http.post('/admin/api/auth/login', { username: 'admin', password: 'wrong' }),
    ).rejects.toMatchObject({ status: 401, message: '用户名或密码错误' } satisfies Partial<ApiError>)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('业务码非 0 抛出带后端 message 的 ApiError', async () => {
    localStorage.setItem(TOKEN_KEY, 'token')
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse(200, { code: 404, message: '配置键不存在: nope', data: null })),
    )

    await expect(http.put('/admin/api/site-config/nope', { value: 'v' })).rejects.toMatchObject({
      code: 404,
      message: '配置键不存在: nope',
    } satisfies Partial<ApiError>)
  })
})
