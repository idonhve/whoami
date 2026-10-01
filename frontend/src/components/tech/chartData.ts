import type { EChartsOption } from 'echarts'

import type { Proficiency, TechItem } from '@/api/tech'
import { PROFICIENCY_LABELS, PROFICIENCY_LEVELS } from '@/api/tech'

/**
 * F2 技术栈图表数据/选项纯函数（Spec 02）。
 * 全在模块顶层、不依赖 DOM，供组件与单测复用：
 *   - 饼图按 category 聚合 weight 占比
 *   - 条状图条形长度映射熟练度档位
 *   - tooltip 展示分类与权重明细
 * 颜色通过参数注入（缺省读设计系统 token），组件与测试各自传入。
 */

export interface ChartColors {
  green: string
  greenGlow: string
  cyan: string
  magenta: string
  amber: string
  text: string
  textDim: string
  border: string
  background: string
}

/** 饼图分类聚合项 */
export interface CategorySlice {
  name: string
  /** 分类内所有条目 weight 之和 */
  value: number
  /** 该分类下的条目明细（tooltip 用） */
  items: TechItem[]
}

/** 条状图条目（熟练度档位已映射为数值） */
export interface BarItem {
  name: string
  category: string
  proficiency: Proficiency
  /** 熟练度档位 1~3 */
  level: number
}

/** 按 category 聚合 weight：返回数组保证顺序稳定（按首次出现的分类顺序） */
export function aggregateByCategory(items: TechItem[]): CategorySlice[] {
  const map = new Map<string, CategorySlice>()
  for (const item of items) {
    const slice = map.get(item.category)
    if (slice) {
      slice.value += item.weight
      slice.items.push(item)
    } else {
      map.set(item.category, { name: item.category, value: item.weight, items: [item] })
    }
  }
  return [...map.values()]
}

/** 熟练度 → 档位数值（条状图条形长度） */
export function proficiencyLevel(proficiency: Proficiency): number {
  return PROFICIENCY_LEVELS[proficiency] ?? 1
}

/** 条状图数据：每个技术项一行，条形长度=档位 */
export function toBarItems(items: TechItem[]): BarItem[] {
  return items.map((item) => ({
    name: item.name,
    category: item.category,
    proficiency: item.proficiency,
    level: proficiencyLevel(item.proficiency),
  }))
}

/** 饼图：按分类聚合权重，tooltip 展示该分类下的技术项明细 */
export function buildPieOption(items: TechItem[], colors: ChartColors): EChartsOption {
  const slices = aggregateByCategory(items)
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: colors.background,
      borderColor: colors.border,
      textStyle: { color: colors.text, fontSize: 13 },
      formatter: (p: unknown) => {
        const item = p as { name: string; value: number; percent: number; data: CategorySlice }
        const detail = item.data.items.map((it) => `  · ${it.name} (x${it.weight})`).join('\n')
        return `<b>${item.name}</b> ${item.value} · ${item.percent}%<br/>${detail}`
      },
    },
    legend: {
      bottom: 0,
      textStyle: { color: colors.text, fontSize: 12 },
      inactiveColor: colors.border,
    },
    series: [
      {
        type: 'pie',
        radius: ['46%', '72%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: colors.border,
          borderWidth: 2,
        },
        label: { color: colors.text, fontSize: 12 },
        emphasis: { scaleSize: 6 },
        data: slices.map((slice, i) => ({
          name: slice.name,
          value: slice.value,
          data: slice,
          itemStyle: { color: sliceColors(colors)[i % sliceColors(colors).length] },
        })),
      },
    ],
  }
}

/** 条状图：条形渐变发光填充（linearGradient），本页唯一 3D/重动效锚点 */
export function buildBarOption(items: TechItem[], colors: ChartColors): EChartsOption {
  const data = toBarItems(items)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: colors.background,
      borderColor: colors.border,
      textStyle: { color: colors.text, fontSize: 13 },
      formatter: (p: unknown) => {
        const params = p as { name: string; data: BarItem }[]
        const first = params[0]
        const label = PROFICIENCY_LABELS[first.data.proficiency]
        return `<b>${first.name}</b><br/>分类：${first.data.category}<br/>熟练度：${label}（${
          first.data.level
        }/3）`
      },
    },
    grid: { left: 8, right: 28, top: 8, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      min: 0,
      max: 3,
      interval: 1,
      splitLine: { lineStyle: { color: colors.border } },
      axisLabel: { color: colors.textDim },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: data.map((d) => d.name),
      axisLabel: { color: colors.text, fontSize: 13 },
      axisLine: { lineStyle: { color: colors.border } },
      axisTick: { show: false },
    },
    series: [
      {
        name: '熟练度',
        type: 'bar',
        data: data.map((d) => ({
          value: d.level,
          name: d.name,
          data: d,
        })),
        barWidth: '40%',
        itemStyle: {
          borderRadius: [0, 2, 2, 0],
          color: (p: unknown) => {
            const { dataIndex } = p as { dataIndex: number }
            return {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 0,
              colorStops: glowStops(colors, dataIndex % 2 === 0),
            }
          },
          shadowColor: colors.greenGlow,
          shadowBlur: 12,
          shadowOffsetX: 2,
        },
      },
    ],
  }
}

function glowStops(colors: ChartColors, even: boolean): { offset: number; color: string }[] {
  if (even) {
    return [
      { offset: 0, color: colors.green },
      { offset: 1, color: colors.cyan },
    ]
  }
  return [
    { offset: 0, color: colors.cyan },
    { offset: 1, color: colors.green },
  ]
}

const sliceColors = (colors: ChartColors): string[] => [
  colors.green,
  colors.cyan,
  colors.magenta,
  colors.amber,
  colors.text,
  colors.greenGlow,
]