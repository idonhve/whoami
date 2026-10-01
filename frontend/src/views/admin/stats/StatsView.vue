<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

import ChinaMap from '@/components/about/ChinaMap.vue'
import { readCssVar } from '@/components/tech/chartTheme'
import { useChart } from '@/components/tech/useChart'
import {
  fetchAdminDailyStats,
  fetchAdminReferrers,
  fetchAdminStatsGeo,
  fetchAdminTopPages,
  type DailyStat,
  type GeoProvince,
  type ReferrerStat,
  type TopPage,
} from '@/api/stats'

import { buildDailyLineOption, buildReferrersOption, buildTopPagesOption } from './statsOptions'

/**
 * 统计看板（Spec 05 后台）：按日 PV/UV 双线（days 可调）、TOP 页面、来源分布、访客地图。
 * 空数据兜底：各卡片显示占位文案；图表 option 构建在 statsOptions.ts（纯函数已测）。
 */

const DAYS_OPTIONS = [7, 30, 90] as const

const days = ref<number>(30)
const loading = ref(true)
const errorMsg = ref('')

const daily = ref<DailyStat[]>([])
const topPages = ref<TopPage[]>([])
const referrers = ref<ReferrerStat[]>([])
const geo = ref<GeoProvince[]>([])

const chartColors = computed(() => {
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
  }
})

function useChartBlock(selector: () => unknown) {
  const container = ref<HTMLElement | null>(null)
  const { render } = useChart(container, {
    register: (echarts) => {
      echarts.use([
        LineChart,
        BarChart,
        PieChart,
        GridComponent,
        LegendComponent,
        TooltipComponent,
        CanvasRenderer,
      ])
    },
  })
  // 后台看板不做"进视口才渲染"门控（容器在 loading 结束后才挂载，
  // useInView 的 onMounted 观察不到）：ref 绑定或数据变化时直接渲染
  watch(
    [container, selector] as const,
    ([el]) => {
      if (el) void render(() => selector() as never)
    },
    { flush: 'post' },
  )
  return { container }
}

const dailyChart = useChartBlock(() =>
  buildDailyLineOption(daily.value, chartColors.value),
)
const topPagesChart = useChartBlock(() =>
  buildTopPagesOption(topPages.value, chartColors.value),
)
const referrersChart = useChartBlock(() =>
  buildReferrersOption(referrers.value, chartColors.value),
)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [dailyData, topData, refData, geoData] = await Promise.all([
      fetchAdminDailyStats(days.value),
      fetchAdminTopPages(days.value),
      fetchAdminReferrers(days.value),
      fetchAdminStatsGeo(),
    ])
    daily.value = dailyData
    topPages.value = topData
    referrers.value = refData
    geo.value = geoData
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

watch(days, load)
onMounted(load)
</script>

<template>
  <section class="stats-page">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        VISITOR STATS
      </h1>
      <p class="sub">$ stats --days {{ days }} · PV/UV 曲线 · TOP 页面 · 来源分布 · 访客地图</p>
      <div class="days-switch" role="group" aria-label="统计窗口天数">
        <button
          v-for="d in DAYS_OPTIONS"
          :key="d"
          type="button"
          class="day-btn"
          :class="{ active: days === d }"
          @click="days = d"
        >
          {{ d }}d
        </button>
      </div>
    </header>

    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>
    <p v-if="loading" class="loading dim">loading ...</p>

    <template v-else>
      <div class="panel">
        <h2 class="panel-title">PV / UV（近 {{ days }} 天）</h2>
        <template v-if="daily.length > 0">
          <div
            :ref="dailyChart.container"
            class="chart"
            style="height: 300px"
            role="img"
            aria-label="按日 PV/UV 双线曲线"
          ></div>
        </template>
        <p v-else class="empty dim">暂无按日数据 · 等待第一批访客</p>
      </div>

      <div class="grid-2">
        <div class="panel">
          <h2 class="panel-title">TOP 页面</h2>
          <template v-if="topPages.length > 0">
            <div
              :ref="topPagesChart.container"
              class="chart"
              style="height: 320px"
              role="img"
              aria-label="TOP 页面条状图"
            ></div>
          </template>
          <p v-else class="empty dim">暂无页面浏览数据</p>
        </div>

        <div class="panel">
          <h2 class="panel-title">来源分布</h2>
          <template v-if="referrers.length > 0">
            <div
              :ref="referrersChart.container"
              class="chart"
              style="height: 320px"
              role="img"
              aria-label="来源分布饼图"
            ></div>
          </template>
          <p v-else class="empty dim">暂无外链来源（直接访问不计）</p>
        </div>
      </div>

      <div class="panel">
        <h2 class="panel-title">访客地图（省级聚合）</h2>
        <ChinaMap :provinces="geo" height="380px" />
      </div>
    </template>
  </section>
</template>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-family: var(--font-pixel);
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 2px;
  color: var(--green);
  text-shadow: 0 0 10px var(--green-glow);
}

.led {
  width: 10px;
  height: 10px;
  background: var(--green);
  box-shadow: 0 0 10px var(--green-glow);
  animation: led-pulse 2.4s ease-in-out infinite;
}

.sub {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 20px;
  letter-spacing: 0.5px;
}

.days-switch {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.day-btn {
  min-height: 44px;
  padding: 6px 16px;
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text-dim);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 13px;
  transition:
    border-color 0.2s,
    color 0.2s,
    box-shadow 0.2s;
}

.day-btn:hover {
  border-color: var(--cyan);
  color: var(--cyan);
}

.day-btn.active {
  border-color: var(--green);
  color: var(--green);
  box-shadow: 0 0 10px var(--green-soft);
}

.error {
  margin: 0;
  color: var(--error);
  font-size: 13px;
}

.dim {
  color: var(--text-dim);
}

.loading {
  margin: 0;
}

.panel {
  padding: 16px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
}

.panel-title {
  margin: 0 0 12px;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 1px;
  color: var(--cyan);
}

.chart {
  width: 100%;
}

.empty {
  margin: 0;
  padding: 32px 0;
  text-align: center;
  font-family: var(--font-term);
  font-size: 18px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

@media (max-width: 1080px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }
}
</style>
