<script setup lang="ts">
import { computed, ref } from 'vue'

import type { Experience } from '@/api/experience'
import AchievementCounter from './AchievementCounter.vue'
import RadarChart from './RadarChart.vue'
import TechTagCloud from './TechTagCloud.vue'
import { defaultSummary, formatDateRange } from './experienceFormat'

/**
 * 经历卡（Spec 09）：默认态为纯视觉卡片——公司/职位/时间摘要（≤30 字）+ 战果翻牌 + 雷达 + 技术标签云；
 * 点击/hover 展开才显示补充要点列表（单条 ≤50 字，不写段落）。
 */
const props = defineProps<{ experience: Experience }>()

const expanded = ref(false)

const summary = computed(() => defaultSummary(props.experience))
const range = computed(() => formatDateRange(props.experience.startDate, props.experience.endDate))

function toggle() {
  expanded.value = !expanded.value
}

/** hover 展开只对悬停设备生效；触屏 tap 由 click 切换，避免手指误触即展开/再收起 */
function supportsHover(): boolean {
  return typeof window !== 'undefined' && window.matchMedia?.('(hover: hover)').matches
}

function onHoverIn() {
  if (supportsHover()) expanded.value = true
}

function onHoverOut() {
  if (supportsHover()) expanded.value = false
}
</script>

<template>
  <article
    class="exp-card"
    :class="{ '--expanded': expanded }"
    @pointerenter="onHoverIn"
    @pointerleave="onHoverOut"
  >
    <header class="exp-head">
      <button
        type="button"
        class="exp-toggle"
        :aria-expanded="expanded"
        aria-controls="highlights"
        @click="toggle"
      >
        <span class="exp-summary" data-testid="exp-summary">{{ summary }}</span>
        <span class="exp-chev" aria-hidden="true">{{ expanded ? '[−]' : '[+]' }}</span>
      </button>
      <p class="exp-company" data-testid="exp-company">{{ experience.company }}</p>
      <p class="exp-meta">
        <span class="exp-title">{{ experience.title }}</span>
        <span class="exp-range">{{ range }}</span>
      </p>
    </header>

    <div class="exp-visual">
      <ul v-if="experience.achievements.length" class="ach-list" data-testid="achievements">
        <li v-for="(a, i) in experience.achievements" :key="i" class="ach">
          <AchievementCounter :value="a.value" />
          <span class="ach-context">{{ a.context }}</span>
        </li>
      </ul>
      <div class="exp-charts">
        <div class="exp-radar">
          <RadarChart :radar="experience.radar" />
        </div>
        <div class="exp-tags">
          <TechTagCloud :tech-tags="experience.techTags" />
        </div>
      </div>
    </div>

    <section v-if="expanded" id="highlights" class="exp-details" data-testid="exp-highlights">
      <h3 class="details-title">DETAILS</h3>
      <ul class="hl-list">
        <li v-for="(hl, i) in experience.highlights" :key="i" class="hl-item">
          <span class="hl-marker" aria-hidden="true">▸</span>{{ hl }}
        </li>
      </ul>
    </section>
  </article>
</template>

<style scoped>
.exp-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 18px;
  border: 2px solid var(--border);
  background: var(--bg-panel);
  transition:
    border-color 0.25s,
    box-shadow 0.25s,
    transform 0.25s cubic-bezier(0.23, 1, 0.32, 1);
}

.exp-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.exp-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 0;
  border: 0;
  background: none;
  color: inherit;
  font: inherit;
  cursor: pointer;
  text-align: left;
}

.exp-summary {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.exp-chev {
  font-family: var(--font-pixel);
  font-size: 9px;
  color: var(--green);
  flex: none;
}

.exp-company {
  margin: 2px 0 0;
  font-family: var(--font-pixel);
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
}

.exp-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  font-size: 12px;
}

.exp-title {
  color: var(--cyan);
}

.exp-range {
  color: var(--text-dim);
  font-family: var(--font-mono);
  font-size: 11px;
}

.exp-visual {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ach-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
}

.ach {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ach-context {
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1.5;
}

.exp-charts {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.exp-details {
  border-top: 1px dashed var(--border-bright);
  padding-top: 12px;
}

.details-title {
  margin: 0 0 8px;
  font-family: var(--font-pixel);
  font-size: 9px;
  letter-spacing: 1px;
  color: var(--amber);
}

.hl-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hl-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: var(--text);
  line-height: 1.55;
}

.hl-marker {
  color: var(--green);
  flex: none;
}

@media (max-width: 680px) {
  .exp-charts {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .exp-card {
    transition: none;
  }
  .exp-company {
    text-shadow: none;
  }
}
</style>
