import { http } from '@/api/http'

/** 操作日志项（契约：docs/spec/06-admin-cms.md） */
export interface OpLogItem {
  id: number
  adminUserId: number
  action: string
  resource: string
  resourceId: string | null
  detail: Record<string, unknown> | null
  ip: string
  createdAt: string
}

export interface OpLogPage {
  list: OpLogItem[]
  total: number
}

export function fetchOpLogs(page = 1, size = 20) {
  return http.get<OpLogPage>(`/admin/api/op-logs?page=${page}&size=${size}`)
}
