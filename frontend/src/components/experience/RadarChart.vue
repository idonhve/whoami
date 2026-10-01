<script setup lang="ts">
import { ref, watch } from 'vue'
import { RadarComponent, TooltipComponent } from 'echarts/components'
import { RadarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'

import type { RadarItem } from '@/api/experience'
import { useChart } from '@/components/tech/useChart'
import { resolveChartColors } from '@/components/tech/chartTheme'
import { buildRadarOption, type RadarPalette } from './radarOption'

/**
 * 能力雷达图（Spec 09）：按 radar 数组（3~8 维）渲染该段经历的能力面。
 * 复用 F2 useChart：ECharts 按路由分包（动态 import echarts/core）、滚动进入视口渲染、reduced-motion 关闭动画。
 */
const props = withDefaults(defineProps<{ radar: RadarItem[]; height?: string }>(), {
  height: '220px',
})

const container = ref<HTMLElement | null>(null)

const { visible, render } = useChart(container, {
  register: (echarts) => {
    echarts.use([RadarChart, RadarComponent, TooltipComponent, CanvasRenderer])
  },
})

watch(visible, (v) => {
  if (v) {
    void render(() => buildRadarOption(props.radar, resolveChartColors() as RadarPalette))
  }
})
</script>

<template>
  <div class="radar-reveal" :class="{ '--in': visible }">
    <div
      ref="container"
      class="radar"
      :style="{ height }"
      role="img"
      :aria-label="`能力雷达图，共 ${radar.length} 个维度`"
      data-testid="radar-chart"
    ></div>
  </div>
</template>

<style scoped>
.radar-reveal {
  opacity: 0;
  transform: translateY(10px);
  transition:
    opacity 0.5s cubic-bezier(0.23, 1, 0.32, 1),
    transform 0.5s cubic-bezier(0.23, 1, 0.32, 1);
}

.radar-reveal.--in {
  opacity: 1;
  transform: translateY(0);
}

.radar {
  width: 100%;
  min-height: 140px;
}

@media (prefers-reduced-motion: reduce) {
  .radar-reveal {
    transition: none;
    opacity: 1;
    transform: none;
  }
}
</style>