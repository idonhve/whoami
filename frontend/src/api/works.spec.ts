import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  fetchAdminWorks,
  fetchSyncLogs,
  fetchWorks,
  triggerSync,
  updateWork,
} from '@/api/works'

function mockApiResponse(data: unknown) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ code: 0, message: 'ok', data }),
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('作品模块 API', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('公开列表默认 scope=all', async () => {
    const fetchMock = mockApiResponse([])

    await fetchWorks()

    expect(fetchMock).toHaveBeenCalledWith('/api/projects?scope=all', expect.anything())
  })

  it('公开列表 featured scope 用于首页精选', async () => {
    const fetchMock = mockApiResponse([])

    await fetchWorks('featured')

    expect(fetchMock).toHaveBeenCalledWith('/api/projects?scope=featured', expect.anything())
  })

  it('管理列表携带筛选参数', async () => {
    const fetchMock = mockApiResponse([])

    await fetchAdminWorks({ language: 'Go', pinned: true, hidden: false })

    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/projects?language=Go&pinned=true&hidden=false',
      expect.anything(),
    )
  })

  it('管理列表无筛选时不带 query', async () => {
    const fetchMock = mockApiResponse([])

    await fetchAdminWorks()

    expect(fetchMock).toHaveBeenCalledWith('/admin/api/projects', expect.anything())
  })

  it('更新运营字段走 PUT', async () => {
    const fetchMock = mockApiResponse(null)

    await updateWork(3, { cnTitle: '我的主页', isPinned: true })

    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/projects/3',
      expect.objectContaining({ method: 'PUT' }),
    )
  })

  it('手动同步走 POST 并解析失败原因', async () => {
    const fetchMock = mockApiResponse({
      status: 'failed',
      repoCount: 0,
      hiddenGone: 0,
      message: '未配置 GITHUB_TOKEN',
    })

    const result = await triggerSync()

    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/projects/sync',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(result.status).toBe('failed')
    expect(result.message).toContain('GITHUB_TOKEN')
  })

  it('同步日志带 limit', async () => {
    const fetchMock = mockApiResponse([])

    await fetchSyncLogs(50)

    expect(fetchMock).toHaveBeenCalledWith('/admin/api/projects/sync/logs?limit=50', expect.anything())
  })
})
