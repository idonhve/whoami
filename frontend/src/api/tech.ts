import { http } from '@/api/http'

/**
 * 技术栈（Spec 02）。字段口径见 docs/spec/02-tech-stack.md。
 * proficiency 三档：master(精通)/proficient(熟练)/familiar(了解)。
 */

export type Proficiency = 'master' | 'proficient' | 'familiar'

export interface TechItem {
  id: number
  name: string
  /** devicon 图标名，可空 = 无图标 */
  icon: string | null
  /** 自由分类：前端/后端/数据库/工具/其他… */
  category: string
  proficiency: Proficiency
  /** 饼图权重 1~100 */
  weight: number
  sortOrder: number
}

/** 新增/编辑请求体：无 id */
export type TechItemCreate = Omit<TechItem, 'id'>

export const PROFICIENCY_LABELS: Record<Proficiency, string> = {
  master: '精通',
  proficient: '熟练',
  familiar: '了解',
}

/** 熟练度档位（数值越大表示越精通），条状图映射条形长度 */
export const PROFICIENCY_LEVELS: Record<Proficiency, number> = {
  master: 3,
  proficient: 2,
  familiar: 1,
}

/** 公开技术栈列表（免登录） */
export function fetchTechStack() {
  return http.get<TechItem[]>('/api/tech-stack')
}

/** 后台全量列表（JWT） */
export function fetchAdminTechStack() {
  return http.get<TechItem[]>('/admin/api/tech-stack')
}

/** 新增，返回新建条目 id */
export function createTechItem(data: TechItemCreate) {
  return http.post<{ id: number }>('/admin/api/tech-stack', data)
}

/** 更新 */
export function updateTechItem(id: number, data: TechItemCreate) {
  return http.put<null>(`/admin/api/tech-stack/${id}`, data)
}

/** 删除 */
export function deleteTechItem(id: number) {
  return http.delete<null>(`/admin/api/tech-stack/${id}`)
}