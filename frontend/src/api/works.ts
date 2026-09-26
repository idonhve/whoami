import { http } from '@/api/http'

/**
 * 作品模块 API（契约：docs/spec/04-works.md）。
 * 公开组免登录（前台 /works 与首页精选作品共用）；管理组走 /admin/api（JWT 自动附带）。
 */

/** 公开作品卡片（ProjectCardDTO，camelCase） */
export interface WorkCard {
  id: number
  cnTitle: string | null
  language: string | null
  stargazersCount: number
  forksCount: number
  htmlUrl: string
  pushedAt: string | null
  isPinned: boolean
  sortOrder: number
}

export type WorksScope = 'all' | 'featured'

/** scope=all 全部未隐藏；scope=featured 仅置顶（首页精选作品区） */
export function fetchWorks(scope: WorksScope = 'all') {
  return http.get<WorkCard[]>(`/api/projects?scope=${scope}`)
}

/** 后台作品列表项（ProjectAdminDTO，额外含运营字段） */
export interface WorkAdminItem {
  id: number
  repoId: number
  repoName: string
  fullName: string
  cnTitle: string | null
  descriptionEn: string | null
  language: string | null
  stargazersCount: number
  forksCount: number
  htmlUrl: string
  pushedAt: string | null
  isPinned: boolean
  isHidden: boolean
  sortOrder: number
  lastSyncedAt: string | null
}

export interface WorkAdminFilters {
  language?: string
  pinned?: boolean
  hidden?: boolean
}

/** 后台全量列表（含隐藏），可按语言/置顶/隐藏筛选 */
export function fetchAdminWorks(filters: WorkAdminFilters = {}) {
  const params = new URLSearchParams()
  if (filters.language) params.set('language', filters.language)
  if (filters.pinned !== undefined) params.set('pinned', String(filters.pinned))
  if (filters.hidden !== undefined) params.set('hidden', String(filters.hidden))
  const query = params.toString()
  return http.get<WorkAdminItem[]>(`/admin/api/projects${query ? `?${query}` : ''}`)
}

/** 更新运营字段；置顶第 4 个后端返回 409 */
export function updateWork(id: number, patch: { cnTitle?: string; isPinned?: boolean; isHidden?: boolean; sortOrder?: number }) {
  return http.put<null>(`/admin/api/projects/${id}`, patch)
}

/** 手动同步结果（失败时 200 包络内 status=failed + 原因） */
export interface SyncResult {
  status: 'success' | 'failed'
  repoCount: number
  hiddenGone: number
  message: string | null
}

/** 立即同步 GitHub 仓库（同步执行，失败不抛错而是返回 failed 结果） */
export function triggerSync() {
  return http.post<SyncResult>('/admin/api/projects/sync')
}

/** 同步日志项 */
export interface SyncLogItem {
  id: number
  triggerType: 'scheduled' | 'manual'
  status: 'success' | 'failed'
  repoCount: number
  hiddenGone: number
  message: string | null
  startedAt: string | null
  finishedAt: string | null
}

/** 最近同步日志（默认 20 条） */
export function fetchSyncLogs(limit = 20) {
  return http.get<SyncLogItem[]>(`/admin/api/projects/sync/logs?limit=${limit}`)
}
