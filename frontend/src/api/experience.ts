import { http } from '@/api/http'

/**
 * 工作经历模块（契约：docs/spec/09-experience.md）。
 * 公开组免登录（/experience 页）；管理组走 /admin/api（JWT 自动附带）。
 * 数据结构与后端 ExperienceDTO 同构（camelCase）。
 */

/** 战果条目：数字/文本值 + 一句话语境（value ≤ 20，context ≤ 50） */
export interface AchievementItem {
  value: string
  context: string
}

/** 雷达维度：维度名（≤20 不重复）+ 分值（0~100 整数） */
export interface RadarItem {
  dimension: string
  score: number
}

/** 经历卡（公开与管理接口同构） */
export interface Experience {
  id: number
  company: string
  title: string
  /** ISO-8601 日期 YYYY-MM-DD */
  startDate: string
  /** null = 至今 */
  endDate: string | null
  companyIntro?: string | null
  projectIntro?: string | null
  /** @deprecated Retained only for clients displaying data from the previous API shape. */
  achievements?: AchievementItem[]
  /** @deprecated Retained only for clients displaying data from the previous API shape. */
  radar?: RadarItem[]
  techTags: string[]
  highlights: string[]
  sortOrder: number
}

/** 新增/编辑请求体：无 id */
export type ExperienceCreate = Omit<Experience, 'id'>

/** 公开经历列表（按 sortOrder 升序、startDate 倒序，后端已排好） */
export function fetchExperiences() {
  return http.get<Experience[]>('/api/experiences')
}

/** 后台全量列表（JWT） */
export function fetchAdminExperiences() {
  return http.get<Experience[]>('/admin/api/experiences')
}

/** 新增，返回新建条目 id */
export function createExperience(data: ExperienceCreate) {
  return http.post<{ id: number }>('/admin/api/experiences', data)
}

/** 更新 */
export function updateExperience(id: number, data: ExperienceCreate) {
  return http.put<null>(`/admin/api/experiences/${id}`, data)
}

/** 删除 */
export function deleteExperience(id: number) {
  return http.delete<null>(`/admin/api/experiences/${id}`)
}
