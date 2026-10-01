<script setup lang="ts">
import { computed } from 'vue'

import { iconPath } from './iconPaths'

/**
 * 技术图标：用 devicon 图标名渲染为内联 SVG（不用 emoji）。
 * 已知图标名走 iconPaths 映射；未知名渲染中性回退占位（首字符）。
 */
const props = withDefaults(defineProps<{ icon?: string | null; size?: number }>(), { size: 30 })

const path = computed(() => iconPath(props.icon))
const fallback = computed(() => {
  const name = props.icon?.trim()
  return name ? name[0].toUpperCase() : '?' // ASCII，像素感占位
})
const label = computed(() => props.icon?.trim() || 'tech')
</script>

<template>
  <svg
    class="tech-icon"
    :width="size"
    :height="size"
    :viewBox="path ? '0 0 24 24' : '0 0 28 28'"
    role="img"
    :aria-label="label"
    data-testid="tech-icon"
  >
    <path v-if="path" :d="path" fill="currentColor" />
    <g v-else class="tech-icon--fallback" :data-testid="`fallback-${fallback}`">
      <rect x="1.5" y="5" width="25" height="18" rx="2" />
      <text
        x="14"
        y="19"
        text-anchor="middle"
        font-size="14"
        font-family="var(--font-pixel), monospace"
        fill="currentColor"
      >
        {{ fallback }}
      </text>
    </g>
  </svg>
</template>

<style scoped>
.tech-icon {
  display: inline-block;
  flex: none;
  filter: drop-shadow(0 0 6px var(--green-glow));
  color: var(--green);
  transition:
    color 0.2s,
    filter 0.2s;
}

.tech-icon--fallback {
  stroke: var(--border-bright);
  stroke-width: 1.5;
  color: var(--text-dim);
}
</style>