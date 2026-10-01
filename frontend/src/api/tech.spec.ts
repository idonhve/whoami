import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  createTechItem,
  deleteTechItem,
  fetchAdminTechStack,
  fetchTechStack,
  updateTechItem,
} from '@/api/tech'

type Stub = { ok: boolean; status: number; json: () => Promise<{ code: number; data: unknown }> }

function stubFetch(handler: (path: string, init?: RequestInit) => Stub) {
  vi.stubGlobal('fetch', vi.fn((path: string, init?: RequestInit) => Promise.resolve(handler(path, init))))
}

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

describe('技术栈 API', () => {
  it('公开列表 GET /api/tech-stack（免登录，不带 token 语义）', async () => {
    const calls: { path: string; init?: RequestInit }[] = []
    stubFetch((path, init) => {
      calls.push({ path, init })
      return { ok: true, status: 200, json: () => Promise.resolve({ code: 0, data: [] }) }
    })

    await fetchTechStack()
    expect(calls[0].path).toBe('/api/tech-stack')
    expect((calls[0].init?.method ?? 'GET').toUpperCase()).toBe('GET')
  })

  it('后台列表 GET /admin/api/tech-stack', async () => {
    const paths: string[] = []
    stubFetch((path) => {
      paths.push(path)
      return { ok: true, status: 200, json: () => Promise.resolve({ code: 0, data: [] }) }
    })
    await fetchAdminTechStack()
    expect(paths[0]).toBe('/admin/api/tech-stack')
  })

  it('新增 POST 请求体形状 = TechItemCreate（无 id）', async () => {
    const calls: { init?: RequestInit }[] = []
    stubFetch((_path, init) => {
      calls.push({ init })
      return { ok: true, status: 200, json: () => Promise.resolve({ code: 0, data: { id: 7 } }) }
    })

    const payload = {
      name: 'Vue 3',
      icon: 'vuejs',
      category: '前端',
      proficiency: 'master' as const,
      weight: 8,
      sortOrder: 1,
    }
    const result = await createTechItem(payload)

    expect(result.id).toBe(7)
    expect(calls[0].init?.method).toBe('POST')
    const sent = JSON.parse(calls[0].init?.body as string)
    expect(sent).toEqual(payload)
    expect(sent).not.toHaveProperty('id')
  })

  it('更新 PUT /admin/api/tech-stack/{id} 携带完整请求体', async () => {
    const calls: { init?: RequestInit }[] = []
    stubFetch((_path, init) => {
      calls.push({ init })
      return { ok: true, status: 200, json: () => Promise.resolve({ code: 0, data: null }) }
    })
    await updateTechItem(
      5,
      { name: 'MySQL', icon: null, category: '数据库', proficiency: 'proficient', weight: 6, sortOrder: 2 },
    )
    expect(calls[0].init?.method).toBe('PUT')
    expect(JSON.parse(calls[0].init?.body as string)).toMatchObject({ name: 'MySQL', weight: 6 })
  })

  it('删除 DELETE /admin/api/tech-stack/{id}', async () => {
    const calls: { init?: RequestInit }[] = []
    stubFetch((_path, init) => {
      calls.push({ init })
      return { ok: true, status: 200, json: () => Promise.resolve({ code: 0, data: null }) }
    })
    await deleteTechItem(9)
    expect(calls[0].init?.method).toBe('DELETE')
  })
})