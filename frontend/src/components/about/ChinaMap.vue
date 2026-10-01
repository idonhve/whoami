<script setup lang="ts">
import { ref, watch } from 'vue'
import { EffectScatterChart, MapChart, ScatterChart } from 'echarts/charts'
import { TooltipComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

import type { GeoProvince } from '@/api/stats'
import { readCssVar } from '@/components/tech/chartTheme'
import { useChart } from '@/components/tech/useChart'
import chinaRaw from '@/assets/geo/china.json?raw'

import { buildChinaMapOption, type MapChartColors } from './chinaMapData'
import { extractGeoProvinceNames, parseChinaGeo } from './chinaGeo'

/**
 * 访客地图（Spec 05，前台 /about 与后台看板同源复用）：
 * - GeoJSON 采用阿里 DataV 标准版图（含台湾、南海诸岛九段线），版本锁定入库，不依赖 CDN
 * - ECharts map 省级 choropleth + effectScatter 省会呼吸打点（本页唯一重动效锚点）
 * - hover tooltip：该省访问 N 次 + 城市 TOP5；境外 / 无数据省份不着色
 */
const props = withDefaults(
  defineProps<{
    provinces: GeoProvince[]
    height?: string
    loading?: boolean
  }>(),
  { height: '420px', loading: false },
)

const container = ref<HTMLElement | null>(null)

const { visible, render } = useChart(container, {
  register: (echarts) => {
    echarts.use([
      MapChart,
      EffectScatterChart,
      ScatterChart,
      TooltipComponent,
      VisualMapComponent,
      CanvasRenderer,
    ])
  },
})

const CHINA_GEO_JSON = parseChinaGeo(chinaRaw)
const GEO_NAMES = extractGeoProvinceNames(CHINA_GEO_JSON)
let mapRegistered = false

function colors(): MapChartColors {
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
    panel: c('--bg-raised', '#0d151f'),
    bg: c('--bg-panel', '#090f16'),
  }
}

function reducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
}

function renderMap(list: GeoProvince[]) {
  void render((mod) => {
    if (!mapRegistered) {
      // 入库 GeoJSON 结构在 chinaGeo.ts 已运行时校验；此处断言对齐 ECharts 的 GeoJSON 输入类型
      mod.registerMap('china', CHINA_GEO_JSON as Parameters<typeof mod.registerMap>[1])
      mapRegistered = true
    }
    return buildChinaMapOption({
      provinces: list,
      geoNames: GEO_NAMES,
      colors: colors(),
      reducedMotion: reducedMotion(),
    })
  })
}

watch(
  () => props.provinces,
  (list) => {
    if (visible.value && list.length > 0) renderMap(list)
  },
)

watch(visible, (isVis) => {
  if (isVis && props.provinces.length > 0) renderMap(props.provinces)
})
</script>

<template>
  <div class="china-map" data-testid="china-map">
    <p v-if="loading" class="state dim">loading geo data ...</p>
    <p v-else-if="provinces.length === 0" class="state dim">
      $ ping --visitors · 暂无访问数据，等你来踩第一脚
    </p>
    <template v-else>
      <div
        ref="container"
        class="canvas"
        :style="{ height }"
        role="img"
        :aria-label="`访客地图：${provinces.length} 个省市有访问记录`"
      ></div>
      <p class="legend dim">● 访问打点（大小 = 访问次数） · hover 省份查看城市 TOP5</p>
    </template>
  </div>
</template>

<style scoped>
.china-map {
  width: 100%;
}

.canvas {
  width: 100%;
  min-height: 280px;
}

.state {
  margin: 0;
  padding: 32px 0;
  text-align: center;
  font-family: var(--font-term);
  font-size: 20px;
}

.legend {
  margin: 8px 0 0;
  text-align: center;
  font-size: 12px;
}

.dim {
  color: var(--text-dim);
}
</style>
