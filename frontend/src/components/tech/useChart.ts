import { onBeforeUnmount, type Ref } from 'vue'

import type { EChartsOption } from 'echarts'

import { useInView } from '@/composables/useInView'

type EchartsModule = typeof import('echarts/core')
type EchartInstance = ReturnType<EchartsModule['init']>

/**
 * F2 图表通用骨架（Spec 02）：
 *  - ECharts 按路由分包（动态 import echarts/core，不进首包，见 vite manualChunks）
 *  - 滚动进入视口后才 setOption 渲染 → 条形/饼图入场渐入动画天然触发
 *  - ResizeObserver 自适应容器，移动端不横向滚动
 *  - prefers-reduced-motion 时关闭 ECharts 动画（MASTER 铁律）
 * 返回 { visible, render, dispose }。
 */
export function useChart(
  container: Ref<HTMLElement | null>,
  options: { register: (mod: EchartsModule) => void },
) {
  const visible = useInView(container)
  let chart: EchartInstance | null = null
  let echartsMod: EchartsModule | null = null
  let resizeObserver: ResizeObserver | null = null

  async function ensureEcharts(): Promise<EchartsModule> {
    if (echartsMod) return echartsMod
    const mod = await import('echarts/core')
    echartsMod = mod
    options.register(mod)
    return mod
  }

  function reduceMotion(): boolean {
    if (typeof window === 'undefined') return false
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }

  async function render(build: (mod: EchartsModule) => EChartsOption) {
    if (!container.value) return
    const mod = await ensureEcharts()
    if (!chart) {
      chart = mod.init(container.value)
      if (typeof ResizeObserver !== 'undefined') {
        resizeObserver = new ResizeObserver(() => chart?.resize())
        resizeObserver.observe(container.value)
      }
    }
    const instance = chart
    if (!instance) return
    const option = build(mod)
    // reduced-motion：关闭动画，结果直出
    if (reduceMotion()) {
      option.animation = false
    }
    instance.setOption(option)
  }

  onBeforeUnmount(() => {
    resizeObserver?.disconnect()
    chart?.dispose()
    chart = null
  })

  return { visible, render }
}