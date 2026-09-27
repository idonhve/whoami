import { http } from '@/api/http'

/**
 * 留言板 API（契约：docs/spec/05-visitor-stats.md）。
 * 公开组免登录：提交（同 IP 每分钟 ≤ 3 条，超限 429）+ 仅 approved 列表（不含 email/IP）。
 * 管理组走 /admin/api（JWT 自动附带）：全量列表（IP 已脱敏）/回复/上下架/删除。
 */

/** 公开留言 DTO（GET /api/messages）：契约红线——绝不含 email 与 IP */
export interface MessageItem {
  id: number
  nickname: string
  content: string
  reply: string | null
  repliedAt: string | null
  createdAt: string
}

export type MessageStatus = 'approved' | 'hidden'

/** 后台留言 DTO（GET /admin/api/messages）：email 仅站主可见，ip 已脱敏（如 1.2.*.*） */
export interface AdminMessageItem {
  id: number
  nickname: string
  email: string | null
  content: string
  status: MessageStatus
  reply: string | null
  repliedAt: string | null
  ip: string
  createdAt: string
}

/** 公开留言列表（仅 approved，limit 默认 20） */
export function fetchPublicMessages(limit = 20): Promise<MessageItem[]> {
  return http.get<MessageItem[]>(`/api/messages?limit=${limit}`)
}

export interface MessageSubmitPayload {
  nickname: string
  content: string
  email?: string
}

/** 提交留言（校验失败 400 / 限流 429 由调用方按 ApiError 处理） */
export function submitMessage(payload: MessageSubmitPayload): Promise<null> {
  return http.post<null>('/api/messages', payload)
}

/** 后台留言列表（status 缺省 = 全量含状态） */
export function fetchAdminMessages(status?: MessageStatus): Promise<AdminMessageItem[]> {
  const query = status ? `?status=${status}` : ''
  return http.get<AdminMessageItem[]>(`/admin/api/messages${query}`)
}

/** 回复留言（reply 可空 = 清除回复） */
export function replyMessage(id: number, reply: string): Promise<null> {
  return http.put<null>(`/admin/api/messages/${id}/reply`, { reply })
}

/** 上下架留言（approved / hidden） */
export function updateMessageStatus(id: number, status: MessageStatus): Promise<null> {
  return http.put<null>(`/admin/api/messages/${id}/status`, { status })
}

/** 删除留言 */
export function deleteMessage(id: number): Promise<null> {
  return http.delete<null>(`/admin/api/messages/${id}`)
}
