import { ApiError, http, TOKEN_KEY } from '@/api/http'
import { apiUrl } from '@/api/base'
import type { ApiResult } from '@/types/api'

/**
 * 证书模块 API（契约：docs/spec/08-awards.md）。
 * 公开组免登录（/awards 缩略图 + 灯箱原图按需加载）；管理组走 /admin/api（JWT 自动附带）。
 * 数据结构与后端 CertificateDTO 同构（camelCase）。
 */

/** 证书条目（公开与管理接口同构） */
export interface Certificate {
  id: number
  name: string
  /** ISO-8601 日期 YYYY-MM-DD */
  obtainedAt: string
  /** 网格媒体预览：图片约 400px 缩略图，PDF 为原文件第一页 */
  thumbUrl: string
  /** 灯箱媒体：压缩原图或 PDF 原文件 */
  imageUrl: string
  sortOrder: number
}

/** 编辑请求体（字段均可选，后端只更新出现的字段） */
export type CertificateUpdate = Partial<Pick<Certificate, 'name' | 'obtainedAt' | 'sortOrder'>>

/** 允许的图片 MIME（后端另有魔数校验，前端先挡一道明显不合法的） */
const ALLOWED_TYPES: readonly string[] = ['image/jpeg', 'image/png', 'image/webp']

/** 单张上限 5MB（与后端校验一致） */
export const CERT_MAX_SIZE_BYTES = 5 * 1024 * 1024

/** 公开列表（后端已按 sortOrder 升序、obtainedAt 倒序排好）；/uploads 地址补全为后端基址 */
export async function fetchCertificates(): Promise<Certificate[]> {
  const list = await http.get<Certificate[]>('/api/certificates')
  return list.map((item) => ({
    ...item,
    thumbUrl: apiUrl(item.thumbUrl),
    imageUrl: apiUrl(item.imageUrl),
  }))
}

/**
 * multipart 上传：http.post 固定 JSON.stringify 不吃 FormData，
 * 这里走原生 fetch，token 读取与响应包络解析口径与 http.ts 一致
 * （multipart 边界由浏览器生成，不手写 Content-Type）。
 */
async function postForm<T>(path: string, form: FormData): Promise<T> {
  const headers: Record<string, string> = {}
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(apiUrl(path), { method: 'POST', headers, body: form })

  let body: ApiResult<T> | null = null
  try {
    body = (await response.json()) as ApiResult<T>
  } catch {
    // 非 JSON 响应按失败处理
  }
  if (!response.ok || !body || body.code !== 0) {
    throw new ApiError(
      response.status,
      body?.code ?? response.status,
      body?.message ?? `请求失败（HTTP ${response.status}）`,
    )
  }
  return body.data as T
}

/** 后台上传新证书（服务端自动生成缩略图与压缩原图），返回新建条目 id */
export function createCertificate(file: File, name: string, obtainedAt: string) {
  const form = new FormData()
  form.append('file', file)
  form.append('name', name)
  form.append('obtainedAt', obtainedAt)
  return postForm<{ id: number }>('/admin/api/certificates', form)
}

/** 更新名称/获取时间/排序 */
export function updateCertificate(id: number, patch: CertificateUpdate) {
  return http.put<null>(`/admin/api/certificates/${id}`, patch)
}

/** 删除（服务端把物理文件一并删除） */
export function deleteCertificate(id: number) {
  return http.delete<null>(`/admin/api/certificates/${id}`)
}

export function isPdfCertificate(item: Pick<Certificate, 'imageUrl'>): boolean {
  return item.imageUrl.split(/[?#]/, 1)[0]?.toLowerCase().endsWith('.pdf') ?? false
}

export function pdfPreviewUrl(url: string): string {
  return `${url.split('#', 1)[0]}#page=1&view=FitH&toolbar=0`
}

/** 上传文件前端预校验（类型/大小）：不合法返回错误文案，合法返回 null */
export function validateCertificateFile(file: File): string | null {
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!ALLOWED_TYPES.includes(file.type) && !isPdf) {
    return '仅支持 jpg / jpeg / png / webp 图片或 PDF 文件'
  }
  if (file.size > CERT_MAX_SIZE_BYTES) {
    return '证书文件大小不能超过 5MB'
  }
  return null
}
