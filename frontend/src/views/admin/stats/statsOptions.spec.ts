import { describe, expect, it } from 'vitest'

import type { DailyStat, ReferrerStat, TopPage } from '@/api/stats'
import { buildDailyLineOption, buildReferrersOption, buildTopPagesOption } from './statsOptions'

const COLORS = {
  green: '#00ff9c',
  greenGlow: 'rgba(0,255,156,.35)',
  cyan: '#2bd9ff',
  magenta: '#ff2e88',
  amber: '#ffb800',
  text: '#c8d6e5',
  textDim: '#64788f',
  border: '#16222f',
}

const DAILY: DailyStat[] = [
  { date: '2026-09-25', pv: 30, uv: 12 },
  { date: '2026-09-26', pv: 45, uv: 20 },
  { date: '2026-09-27', pv: 0, uv: 0 },
]

/** 空数据返回 null 已单独断言；此处窄化非空类型供后续字段断言 */
function requireOption<T>(option: T | null): T {
  if (option === null) throw new Error('option 不应为 null')
  return option
}

describe('PV/UV 双线曲线', () => {
  it('空数据返回 null（视图层走占位文案兜底）', () => {
    expect(buildDailyLineOption([], COLORS)).toBeNull()
  })

  it('双系列：PV 绿线 + UV 青线，x 轴为日期', () => {
    const option = requireOption(buildDailyLineOption(DAILY, COLORS))
    const series = option.series as { name: string; data: number[]; type: string }[]
    expect(series).toHaveLength(2)
    expect(series[0].name).toBe('PV')
    expect(series[0].data).toEqual([30, 45, 0])
    expect(series[1].name).toBe('UV')
    expect(series[1].data).toEqual([12, 20, 0])
    expect(option.xAxis.data).toEqual(['2026-09-25', '2026-09-26', '2026-09-27'])
  })
})

describe('TOP 页面条状图', () => {
  it('空数据返回 null', () => {
    expect(buildTopPagesOption([], COLORS)).toBeNull()
  })

  it('水平条反转：访问量最高的页面排最上', () => {
    const pages: TopPage[] = [
      { pagePath: '/about', pv: 40 },
      { pagePath: '/', pv: 25 },
      { pagePath: '/works', pv: 10 },
    ]
    const option = requireOption(buildTopPagesOption(pages, COLORS))
    const yAxis = option.yAxis as { data: string[] }
    expect(yAxis.data).toEqual(['/works', '/', '/about'])
    expect(option.series[0].data).toEqual([10, 25, 40])
  })

  it('只取前 10 条', () => {
    const pages: TopPage[] = Array.from({ length: 15 }, (_, i) => ({
      pagePath: `/p${i}`,
      pv: 15 - i,
    }))
    const option = requireOption(buildTopPagesOption(pages, COLORS))
    expect((option.yAxis as { data: unknown[] }).data).toHaveLength(10)
  })
})

describe('来源分布饼图', () => {
  it('空数据返回 null', () => {
    expect(buildReferrersOption([], COLORS)).toBeNull()
  })

  it('有数据时环形饼图 data 与来源一致（含调色板配色）', () => {
    const refs: ReferrerStat[] = [
      { referrer: 'https://google.com', count: 9 },
      { referrer: 'https://juejin.cn', count: 4 },
    ]
    const option = requireOption(buildReferrersOption(refs, COLORS))
    const data = option.series[0].data as { name: string; value: number }[]
    expect(data.map((d) => ({ name: d.name, value: d.value }))).toEqual([
      { name: 'https://google.com', value: 9 },
      { name: 'https://juejin.cn', value: 4 },
    ])
  })
})
