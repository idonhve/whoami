<script setup lang="ts">
import { ref, watch } from 'vue'
import { TooltipComponent } from 'echarts/components'
import { BarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'

import type { TechItem } from '@/api/tech'
import { buildBarOption } from './chartData'
import { resolveChartColors } from './chartTheme'
import { useChart } from './useChart'

/**
 * 技术栈水平条状图（Spec 02）：单项熟练度，条形渐变发光填充。
 * 本页唯一 3D/重动效锚点 = 条形渐变发光填充动画（滚动进视口后 ECharts 动画生长）。
 */
const props = withDefaults(defineProps<{ items: TechItem[]; height?: string }>(), {
  height: '360px',
})

const container = ref<HTMLElement | null>(null)

const { visible, render } = useChart(container, {
  register: (echarts) => {
    echarts.use([BarChart, TooltipComponent, CanvasRenderer])
  },
})

watch(visible, (v) => {
  if (v) void render(() => buildBarOption(props.items, resolveChartColors()))
})
</script>

<template>
  <div class="chart-reveal" :class="{ '--in': visible }" data-testid="bar-reveal">
    <div
      ref="container"
      class="chart"
      :style="{ height }"
      role="img"
      :aria-label="`技术栈单项熟练度条状图，共 ${items.length} 项`"
    ></div>
  </div>
</template>

<style scoped>
.chart-reveal {
  opacity: 0;
  transform: translateY(14px);
  transition:
    opacity 0.6s cubic-bezier(0.23, 1, 0.32, 1),
    transform 0.6s cubic-bezier(0.23, 1, 0.32, 1);
}

.chart-reveal.--in {
  opacity: 1;
  transform: translateY(0);
}

.chart {
  width: 100%;
  min-height: 240px;
}

@media (prefers-reduced-motion: reduce) {
  .chart-reveal {
    transition: none;
    opacity: 1;
    transform: none;
  }
}
</style>