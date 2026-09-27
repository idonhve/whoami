import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  createExperience,
  deleteExperience,
  fetchAdminExperiences,
  fetchExperiences,
  updateExperience,
} from '@/api/experience'

function mockApiResponse(data: unknown) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ code: 0, message: 'ok', data }),
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('经历模块 API', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('公开列表走 GET /api/experiences（免登录）', async () => {
    const fetchMock = mockApiResponse([])
    await fetchExperiences()
    expect(fetchMock).toHaveBeenCalledWith('/api/experiences', expect.anything())
  })

  it('后台全量列表走 /admin/api/experiences', async () => {
    const fetchMock = mockApiResponse([])
    await fetchAdminExperiences()
    expect(fetchMock).toHaveBeenCalledWith('/admin/api/experiences', expect.anything())
  })

  it('新增走 POST /admin/api/experiences', async () => {
    const fetchMock = mockApiResponse({ id: 7 })
    const id = await createExperience({
      company: '星云科技',
      title: '前端工程师',
      startDate: '2021-06-01',
      endDate: null,
      achievements: [],
      radar: [
        { dimension: 'a', score: 80 },
        { dimension: 'b', score: 70 },
        { dimension: 'c', score: 60 },
      ],
      techTags: ['vue'],
      highlights: [],
      sortOrder: 0,
    })
    expect(id.id).toBe(7)
    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/experiences',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('更新走 PUT /admin/api/experiences/{id}', async () => {
    const fetchMock = mockApiResponse(null)
    await updateExperience(3, {
      company: 'x',
      title: 'y',
      startDate: '2020-01-01',
      endDate: '2022-01-01',
      achievements: [],
      radar: [
        { dimension: 'a', score: 80 },
        { dimension: 'b', score: 70 },
        { dimension: 'c', score: 60 },
      ],
      techTags: [],
      highlights: [],
      sortOrder: 1,
    })
    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/experiences/3',
      expect.objectContaining({ method: 'PUT' }),
    )
  })

  it('删除走 DELETE /admin/api/experiences/{id}', async () => {
    const fetchMock = mockApiResponse(null)
    await deleteExperience(5)
    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/experiences/5',
      expect.objectContaining({ method: 'DELETE' }),
    )
  })
})
