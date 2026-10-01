<script setup lang="ts">
import { ref, watch } from 'vue'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'

import type { TechItem } from '@/api/tech'
import { buildPieOption } from './chartData'
import { resolveChartColors } from './chartTheme'
import { useChart } from './useChart'

/**
 * 技术栈饼图（Spec 02）：按 category 聚合 weight 占比，hover 出分类与权重明细 tooltip。
 */
const props = withDefaults(defineProps<{ items: TechItem[]; height?: string }>(), {
  height: '340px',
})

const container = ref<HTMLElement | null>(null)

const { visible, render } = useChart(container, {
  register: (echarts) => {
    echarts.use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer])
  },
})

watch(visible, (v) => {
  if (v) void render(() => buildPieOption(props.items, resolveChartColors()))
})
</script>

<template>
  <div class="chart-reveal" :class="{ '--in': visible }" data-testid="pie-reveal">
    <div
      ref="container"
      class="chart"
      :style="{ height }"
      role="img"
      :aria-label="`技术栈分类占比饼图，共 ${items.length} 项`"
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
  min-height: 200px;
}

@media (prefers-reduced-motion: reduce) {
  .chart-reveal {
    transition: none;
    opacity: 1;
    transform: none;
  }
}
</style>