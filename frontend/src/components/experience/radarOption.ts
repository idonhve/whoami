import type { EChartsOption } from 'echarts'

import type { RadarItem } from '@/api/experience'

/** ECharts radar 配色（经 token 读取，fallback 为既定值）。 */
export interface RadarPalette {
  text: string
  textDim: string
  border: string
  background: string
  green: string
  greenGlow: string
}

/**
 * 构造能力雷达图 option（Spec 09）：indicator 数量 = radar 数组维度数（3~8）。
 * 独立成纯函数便于组件测试断言"按 radar 数据渲染维度数"，且不触发 canvas。
 */
export function buildRadarOption(radar: RadarItem[], c: RadarPalette): EChartsOption {
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: c.background,
      borderColor: c.border,
      textStyle: { color: c.text, fontFamily: 'monospace', fontSize: 12 },
    },
    radar: {
      indicator: radar.map((r) => ({ name: r.dimension, max: 100 })),
      radius: '70%',
      splitNumber: 4,
      axisName: { color: c.textDim, fontSize: 11, fontFamily: 'monospace' },
      splitLine: { lineStyle: { color: c.border } },
      splitArea: { show: false },
      axisLine: { lineStyle: { color: c.border } },
    },
    series: [
      {
        type: 'radar',
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { color: c.green, width: 2 },
        itemStyle: { color: c.green },
        areaStyle: { color: c.greenGlow, opacity: 0.25 },
        data: [{ value: radar.map((r) => r.score), name: '能力' }],
      },
    ],
  }
}