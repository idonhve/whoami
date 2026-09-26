import type { Experience, RadarItem } from '@/api/experience'

/**
 * 工作经历前端纯函数（Spec 09）。无 DOM 依赖，便于组件测试对真实数据守门：
 * —— "单卡片默认态文字 ≤ 30 字"由测试直接断言本函数结果，防口径漂移。
 */

/** 解析战果值：拆出前导数字与常显后缀。如 "300%" → {num:300, suffix:"%"}、"50w+" → {num:50, suffix:"w+"}。纯文本（无前导数字）返回 num=null。 */
export function parseCounter(value: string): { num: number | null; suffix: string } {
  const match = /^(-?\d+(?:\.\d+)?)(.*)$/.exec(value.trim())
  if (!match) return { num: null, suffix: value.trim() }
  return { num: Number(match[1]), suffix: match[2] }
}

/** 压缩日期："2021.06 – 至今"（endDate 为 null 视为至今） */
export function formatDateRange(startDate: string, endDate: string | null): string {
  const fmt = (iso: string) => iso.slice(0, 7).replace('-', '.')
  const end = endDate ? fmt(endDate) : '至今'
  return `${fmt(startDate)} – ${end}`
}

/**
 * 单卡片默认态摘要：company · title · 时间。≤30 字由测试守门；超长在此截断兜底。
 */
export function defaultSummary(
  e: Pick<Experience, 'company' | 'title' | 'startDate' | 'endDate'>,
): string {
  const joined = `${e.company} · ${e.title} · ${formatDateRange(e.startDate, e.endDate)}`
  return joined.length > 30 ? `${joined.slice(0, 29)}…` : joined
}

/** 校验雷达维度数组：3~8 维、维度名 ≤20、不重复、score 0~100 整数。返回错误文案或 null。 */
export function validateRadar(radar: RadarItem[]): string | null {
  if (radar.length < 3 || radar.length > 8) return '雷达维度数应为 3~8'
  const seen = new Set<string>()
  for (const item of radar) {
    const name = item.dimension.trim()
    if (!name) return '雷达维度名不能为空'
    if (name.length > 20) return '雷达维度名最长 20'
    if (seen.has(name)) return `雷达维度名「${name}」重复`
    seen.add(name)
    if (!Number.isInteger(item.score) || item.score < 0 || item.score > 100) {
      return `维度「${name}」分值应为 0~100 的整数`
    }
  }
  return null
}
