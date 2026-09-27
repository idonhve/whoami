import { describe, expect, it } from 'vitest'

import type { TechItem } from '@/api/tech'
import {
  aggregateByCategory,
  buildBarOption,
  buildPieOption,
  toBarItems,
  type ChartColors,
} from './chartData'

const COLORS: ChartColors = {
  green: '#00ff9c',
  greenGlow: 'rgba(0,255,156,0.35)',
  cyan: '#2bd9ff',
  magenta: '#ff2e88',
  amber: '#ffb800',
  text: '#c8d6e5',
  textDim: '#64788f',
  border: '#16222f',
  background: '#090f16',
}

function item(partial: Partial<TechItem>): TechItem {
  return {
    id: 1,
    name: 'X',
    icon: null,
    category: '工具',
    proficiency: 'master',
    weight: 5,
    sortOrder: 1,
    ...partial,
  }
}

describe('饼图数据（按 category 聚合 weight）', () => {
  it('相同分类的 weight 求和，顺序按首次出现', () => {
    const slices = aggregateByCategory([
      item({ category: '前端', weight: 8, name: 'Vue' }),
      item({ category: '后端', weight: 6, name: 'Java' }),
      item({ category: '前端', weight: 4, name: 'React' }),
      item({ category: '工具', weight: 2, name: 'Docker' }),
    ])

    expect(slices.map((s) => [s.name, s.value])).toEqual([
      ['前端', 12],
      ['后端', 6],
      ['工具', 2],
    ])
    expect(slices[0].items.map((i) => i.name)).toEqual(['Vue', 'React'])
  })

  it('饼图 option 引用聚合后的分类占比，tooltip 含条目明细', () => {
    const option = buildPieOption(
      [item({ category: '前端', weight: 3, name: 'Vue' }), item({ category: '前端', weight: 7, name: 'React' })],
      COLORS,
    )

    const series = (Array.isArray(option.series) ? option.series[0] : option.series) as { data: { name: string; value: number; data: { items: { name: string }[] } }[] }
    expect(series.data).toHaveLength(1)
    expect(series.data[0].name).toBe('前端')
    expect(series.data[0].value).toBe(10)

    const tooltip = option.tooltip as { formatter: (p: unknown) => string }
    const html = tooltip.formatter({
      name: '前端',
      value: 10,
      percent: 100,
      data: { items: [{ name: 'Vue', weight: 3 }, { name: 'React', weight: 7 }] },
    })
    expect(html).toContain('前端')
    expect(html).toContain('· Vue (x3)')
    expect(html).toContain('· React (x7)')
  })
})

describe('条状图（熟练度档位映射）', () => {
  it('master/proficient/familiar 映射为 3/2/1', () => {
    const bars = toBarItems([
      item({ proficiency: 'master', name: 'A' }),
      item({ proficiency: 'proficient', name: 'B' }),
      item({ proficiency: 'familiar', name: 'C' }),
    ])
    expect(bars.map((b) => b.level)).toEqual([3, 2, 1])
  })

  it('条状图 option 条形值=档位，渐变发光填充，tooltip 展示熟练度中文', () => {
    const option = buildBarOption([item({ proficiency: 'master', name: 'Vue', category: '前端' })], COLORS)

    const series = (Array.isArray(option.series) ? option.series[0] : option.series) as { data: { value: number }[]; itemStyle: { color: unknown; shadowBlur: number; shadowColor: string } }
    expect(series.data[0].value).toBe(3)
    // 渐变发光填充（linearGradient）——本页唯一重动效锚点
    expect(series.itemStyle.shadowBlur).toBeGreaterThan(0)
    expect((series.itemStyle.color as (p: unknown) => unknown).length).toBeGreaterThan(0)

    const tooltip = option.tooltip as { formatter: (p: unknown) => string }
    const html = tooltip.formatter([
      { name: 'Vue', data: { name: 'Vue', category: '前端', proficiency: 'master', level: 3 } },
    ])
    expect(html).toContain('Vue')
    expect(html).toContain('前端')
    expect(html).toContain('精通')
  })
})