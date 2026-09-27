import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  fetchAdminDailyStats,
  fetchAdminReferrers,
  fetchAdminStatsGeo,
  fetchAdminTopPages,
  fetchVisitStatsGeo,
} from './stats'

/** 统一响应包络：{ code:0, message:'ok', data } */
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

describe('访客统计 API（Spec 05 契约）', () => {
  it('fetchVisitStatsGeo 调 GET /api/visit-stats/geo（公开，无 IP 字段契约由后端保证）', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([{ province: '广东省', count: 3, cities: [] }]))

    const data = await fetchVisitStatsGeo()

    expect(fetchMock).toHaveBeenCalledWith('/api/visit-stats/geo', expect.anything())
    expect(data).toEqual([{ province: '广东省', count: 3, cities: [] }])
  })

  it('fetchAdminDailyStats(7) 调 GET /admin/api/stats/daily?days=7', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([{ date: '2026-09-27', pv: 5, uv: 2 }]))

    const data = await fetchAdminDailyStats(7)

    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/stats/daily?days=7')
    expect(data).toEqual([{ date: '2026-09-27', pv: 5, uv: 2 }])
  })

  it('fetchAdminDailyStats 缺省 days=30', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([]))

    await fetchAdminDailyStats()

    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/stats/daily?days=30')
  })

  it('fetchAdminTopPages 调 GET /admin/api/stats/top-pages?days=30', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([{ pagePath: '/about', pv: 9 }]))

    const data = await fetchAdminTopPages(30)

    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/stats/top-pages?days=30')
    expect(data[0].pagePath).toBe('/about')
  })

  it('fetchAdminReferrers 调 GET /admin/api/stats/referrers?days=30', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([{ referrer: 'https://google.com', count: 2 }]))

    const data = await fetchAdminReferrers(30)

    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/stats/referrers?days=30')
    expect(data[0].count).toBe(2)
  })

  it('fetchAdminStatsGeo 调 GET /admin/api/stats/geo（与公开 geo 同源全量）', async () => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(okResponse([]))

    await fetchAdminStatsGeo()

    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/stats/geo')
  })
})
