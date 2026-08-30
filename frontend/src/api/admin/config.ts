import { http } from '@/api/http'

/** 后台全量配置项（契约：docs/spec/06-admin-cms.md） */
export interface SiteConfigItem {
  key: string
  value: string | null
  description: string | null
  updatedAt: string
}

export function fetchAdminSiteConfigs() {
  return http.get<SiteConfigItem[]>('/admin/api/site-config')
}

/** 更新已有键；键不存在后端返回 404 */
export function updateSiteConfig(key: string, value: string) {
  return http.put<null>(`/admin/api/site-config/${encodeURIComponent(key)}`, { value })
}
