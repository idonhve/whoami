<script setup lang="ts">
import { computed, ref } from 'vue'

import type { Experience } from '@/api/experience'
import TechTagCloud from './TechTagCloud.vue'
import { defaultSummary, formatDateRange } from './experienceFormat'

/**
 * 经历卡：展示公司介绍、项目介绍与技术标签；点击/hover 展开补充要点。
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
      <div v-if="experience.companyIntro?.trim() || experience.projectIntro?.trim()" class="exp-intros">
        <section v-if="experience.companyIntro?.trim()" class="intro-panel">
          <h3 class="intro-title">公司介绍</h3>
          <p class="intro-text">{{ experience.companyIntro }}</p>
        </section>
        <section v-if="experience.projectIntro?.trim()" class="intro-panel">
          <h3 class="intro-title">项目介绍</h3>
          <p class="intro-text">{{ experience.projectIntro }}</p>
        </section>
      </div>
      <div v-if="experience.techTags.length" class="exp-tags">
        <TechTagCloud :tech-tags="experience.techTags" />
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

.exp-intros {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.intro-panel {
  min-width: 0;
  border-left: 2px solid var(--border-bright);
  padding-left: 12px;
}

.intro-title {
  margin: 0 0 6px;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--green);
}

.intro-text {
  margin: 0;
  font-size: 12px;
  color: var(--text);
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.exp-tags {
  min-width: 0;
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
  .exp-intros {
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
