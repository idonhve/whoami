import type { ChartColors } from './chartData'

/**
 * 从设计系统 :root 读取颜色 token，供 ECharts 使用（canvas 无法直接消费 CSS 变量）。
 * 铁律：组件不写裸 hex，一律经此读取 token（见 design-system/whoami/MASTER.md）。
 * 解析失败时回退到 token 的既定值，保证运行时可用。
 */
export function readCssVar(name: string, fallback: string): string {
  if (typeof window === 'undefined') return fallback
  const value = window.getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

export function resolveChartColors(): ChartColors {
  const c = (name: string, fb: string) => readCssVar(name, fb)
  return {
    green: c('--green', '#00ff9c'),
    greenGlow: c('--green-glow', 'rgba(0,255,156,0.35)'),
    cyan: c('--cyan', '#2bd9ff'),
    magenta: c('--magenta', '#ff2e88'),
    amber: c('--amber', '#ffb800'),
    text: c('--text', '#c8d6e5'),
    textDim: c('--text-dim', '#64788f'),
    border: c('--border', '#16222f'),
    background: c('--bg-panel', '#090f16'),
  }
}