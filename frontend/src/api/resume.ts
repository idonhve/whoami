import { ApiError, TOKEN_KEY, http } from '@/api/http'
import { apiUrl } from '@/api/base'
import type { ApiResult } from '@/types/api'

/**
 * 简历模块 API（契约：docs/spec/07-resume.md）。
 * 公开组免登录（前台 DownloadButton 显隐/文案）；
 * 管理组走 /admin/api（JWT 自动附带，multipart 上传除外——http 的 JSON 封装不适用，
 * 此处自带最小 fetch 封装，保持与 http.ts 相同的包络与错误口径）。
 *
 * 下载不走本模块：直接用 <a href="/api/resume/download"> 触发浏览器导航下载，
 * 埋点由服务端在下载接口内直写（Spec 07 Testing Decisions）。
 */

/** GET /api/resume/latest 响应：按钮显隐与文案 */
export interface ResumeLatest {
  exists: boolean
  displayName: string | null
  updatedAt: string | null
}

/** 最新简历元信息（公开）；exists=false 当后台无任何版本 */
export function getLatest() {
  return http.get<ResumeLatest>('/api/resume/latest')
}

/** 公开下载地址（服务端生成 Content-Disposition 文件名并写 resume_download 埋点） */
export const RESUME_DOWNLOAD_URL = apiUrl('/api/resume/download')

/** 后台版本列表项（ResumeVersionDTO） */
export interface ResumeVersion {
  id: number
  versionNo: number
  displayName: string
  sizeBytes: number
  isCurrent: boolean
  uploadedAt: string | null
}

/** 后台版本列表（倒序） */
export function fetchAdminResumes() {
  return http.get<ResumeVersion[]>('/admin/api/resumes')
}

/** POST /admin/api/resumes 响应；evictedVersionNos 为本次上传自动淘汰的历史版本号 */
export interface UploadResult {
  id: number
  versionNo: number
  evictedVersionNos: number[]
}

/** 上传新简历版本（multipart/form-data，字段名 file；仅 pdf ≤ 20MB，否则 400） */
export async function uploadResume(file: File): Promise<UploadResult> {
  const form = new FormData()
  form.append('file', file)
  const headers: Record<string, string> = {}
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const response = await fetch(apiUrl('/admin/api/resumes'), { method: 'POST', headers, body: form })
  let body: ApiResult<UploadResult> | null = null
  try {
    body = (await response.json()) as ApiResult<UploadResult>
  } catch {
    // 非 JSON 响应按失败处理（与 http.ts 口径一致）
  }
  if (!response.ok || !body || body.code !== 0) {
    throw new ApiError(
      response.status,
      body?.code ?? response.status,
      body?.message ?? `上传失败（HTTP ${response.status}）`,
    )
  }
  return body.data as UploadResult
}

/** 回滚指定版本为当前版 */
export function restoreResume(id: number) {
  return http.put<null>(`/admin/api/resumes/${id}/restore`)
}
