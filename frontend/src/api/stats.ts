import { http } from '@/api/http'

/**
 * 访客统计 API（契约：docs/spec/05-visitor-stats.md）。
 * 公开组免登录（关于本站访客地图）；管理组走 /admin/api（JWT 由 http 自动附带）。
 * 红线：所有 geo 响应结构层面不含任何 IP 信息（GeoProvinceDTO 同源）。
 */

/** 省内城市聚合条目（访客地图 hover 城市列表，≤ 5 条） */
export interface GeoCity {
  city: string
  count: number
}

/** 省级聚合（公开 /api/visit-stats/geo 与后台 /admin/api/stats/geo 同源） */
export interface GeoProvince {
  province: string
  count: number
  cities: GeoCity[]
}

/** 按日统计：PV = page_view 事件数；UV = 当日去重 sessionId */
export interface DailyStat {
  date: string
  pv: number
  uv: number
}

/** TOP 页面：窗口内 page_view 事件按 pagePath 聚合 */
export interface TopPage {
  pagePath: string
  pv: number
}

/** 来源分布：窗口内 visit_log.referrer 聚合（直接访问不计） */
export interface ReferrerStat {
  referrer: string
  count: number
}

/** 公开访客地图（/about 用）：省级聚合 + 城市 TOP5 */
export function fetchVisitStatsGeo(): Promise<GeoProvince[]> {
  return http.get<GeoProvince[]>('/api/visit-stats/geo')
}

/** 后台按日 PV/UV 曲线（days 缺省 30，服务端上限 365） */
export function fetchAdminDailyStats(days = 30): Promise<DailyStat[]> {
  return http.get<DailyStat[]>(`/admin/api/stats/daily?days=${days}`)
}

/** 后台 TOP 页面（days 缺省 30） */
export function fetchAdminTopPages(days = 30): Promise<TopPage[]> {
  return http.get<TopPage[]>(`/admin/api/stats/top-pages?days=${days}`)
}

/** 后台来源分布（days 缺省 30） */
export function fetchAdminReferrers(days = 30): Promise<ReferrerStat[]> {
  return http.get<ReferrerStat[]>(`/admin/api/stats/referrers?days=${days}`)
}

/** 后台访客地图（与公开 geo 同源全量） */
export function fetchAdminStatsGeo(): Promise<GeoProvince[]> {
  return http.get<GeoProvince[]>('/admin/api/stats/geo')
}
