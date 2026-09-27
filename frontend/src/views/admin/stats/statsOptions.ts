import type { DailyStat, ReferrerStat, TopPage } from '@/api/stats'

/**
 * 后台统计看板 option 构建（Spec 05，纯函数便于单测）。
 * 空数据兜底：空数组返回 null（视图层显示占位文案），不渲染空图。
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
}

const GRID = { left: 48, right: 24, top: 40, bottom: 32, containLabel: true } as const

/** 按日 PV/UV 双线曲线（近 N 天） */
export function buildDailyLineOption(daily: DailyStat[], colors: ChartColors) {
  if (daily.length === 0) return null
  return {
    animation: true,
    tooltip: { trigger: 'axis' as const },
    legend: {
      data: ['PV', 'UV'],
      textStyle: { color: colors.textDim },
      top: 0,
      right: 0,
    },
    grid: { ...GRID },
    xAxis: {
      type: 'category' as const,
      data: daily.map((d) => d.date),
      axisLine: { lineStyle: { color: colors.border } },
      axisLabel: { color: colors.textDim, fontSize: 11 },
    },
    yAxis: {
      type: 'value' as const,
      minInterval: 1,
      axisLine: { lineStyle: { color: colors.border } },
      axisLabel: { color: colors.textDim, fontSize: 11 },
      splitLine: { lineStyle: { color: colors.border, type: 'dashed' as const } },
    },
    series: [
      {
        name: 'PV',
        type: 'line' as const,
        smooth: false,
        symbol: 'rect' as const,
        symbolSize: 6,
        data: daily.map((d) => d.pv),
        itemStyle: { color: colors.green },
        lineStyle: { color: colors.green, shadowColor: colors.greenGlow, shadowBlur: 6 },
        areaStyle: { color: colors.greenGlow, opacity: 0.12 },
      },
      {
        name: 'UV',
        type: 'line' as const,
        smooth: false,
        symbol: 'rect' as const,
        symbolSize: 6,
        data: daily.map((d) => d.uv),
        itemStyle: { color: colors.cyan },
        lineStyle: { color: colors.cyan },
      },
    ],
  }
}

const TOP_PAGES_LIMIT = 10

/** TOP 页面条状图（水平条，取前 10） */
export function buildTopPagesOption(topPages: TopPage[], colors: ChartColors) {
  if (topPages.length === 0) return null
  const rows = topPages.slice(0, TOP_PAGES_LIMIT)
  return {
    animation: true,
    tooltip: { trigger: 'axis' as const, axisPointer: { type: 'shadow' as const } },
    grid: { ...GRID },
    xAxis: {
      type: 'value' as const,
      minInterval: 1,
      axisLabel: { color: colors.textDim, fontSize: 11 },
      splitLine: { lineStyle: { color: colors.border, type: 'dashed' as const } },
    },
    yAxis: {
      type: 'category' as const,
      // ECharts 水平条自下而上绘制，反转让 TOP1 在最上
      data: rows.map((r) => r.pagePath).reverse(),
      axisLabel: { color: colors.text, fontSize: 11 },
      axisLine: { lineStyle: { color: colors.border } },
    },
    series: [
      {
        name: 'PV',
        type: 'bar' as const,
        barMaxWidth: 16,
        data: rows.map((r) => r.pv).reverse(),
        itemStyle: { color: colors.cyan },
        label: { show: true, position: 'right' as const, color: colors.textDim, fontSize: 11 },
      },
    ],
  }
}

/** 来源分布饼图（referrer 聚合；缺省显示占位） */
export function buildReferrersOption(referrers: ReferrerStat[], colors: ChartColors) {
  if (referrers.length === 0) return null
  const palette = [colors.green, colors.cyan, colors.magenta, colors.amber, colors.textDim]
  return {
    animation: true,
    tooltip: { trigger: 'item' as const },
    legend: {
      orient: 'vertical' as const,
      right: 8,
      top: 'middle',
      textStyle: { color: colors.textDim, fontSize: 11 },
    },
    series: [
      {
        name: '来源分布',
        type: 'pie' as const,
        radius: ['42%', '68%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: colors.border, borderWidth: 1 },
        label: { show: false },
        data: referrers.map((r, i) => ({
          name: r.referrer,
          value: r.count,
          itemStyle: { color: palette[i % palette.length] },
        })),
      },
    ],
  }
}
