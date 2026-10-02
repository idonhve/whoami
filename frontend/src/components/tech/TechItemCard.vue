<script setup lang="ts">
import type { Proficiency, TechItem } from '@/api/tech'

import TechIcon from './TechIcon.vue'
import TechProficiencyLabel from './TechProficiencyLabel.vue'

/**
 * 技术项列表项：名称 + devicon 图标 + 熟练度标签（Spec 02）。
 */
withDefaults(
  defineProps<{
    item: TechItem
    /** 进入视口后置位，配合入场渐入动画（页面级统一控制） */
    visible?: boolean
    index?: number
  }>(),
  { visible: true, index: 0 },
)
</script>

<template>
  <li class="tech-item" :class="{ '--reveal': visible }" :style="{ '--i': index }">
    <TechIcon :icon="item.icon" :icon-url="item.iconUrl" :name="item.name" :size="30" />
    <div class="meta">
      <span class="name">{{ item.name }}</span>
      <span class="cat">{{ item.category }}</span>
    </div>
    <div class="right">
      <TechProficiencyLabel :proficiency="item.proficiency as Proficiency" />
      <span class="weight">W{{ item.weight }}</span>
    </div>
  </li>
</template>

<style scoped>
.tech-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-radius: 0;
  opacity: 0;
  transform: translateY(12px);
  transition:
    opacity 0.45s cubic-bezier(0.23, 1, 0.32, 1),
    transform 0.45s cubic-bezier(0.23, 1, 0.32, 1),
    border-color 0.2s;
  transition-delay: calc(var(--i) * 40ms);
}

.tech-item:hover {
  border-color: var(--border-bright);
}

.tech-item.--reveal {
  opacity: 1;
  transform: translateY(0);
}

.meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name {
  color: var(--text);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat {
  color: var(--text-dim);
  font-size: 12px;
}

.right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.weight {
  color: var(--text-dim);
  font-size: 12px;
}

@media (prefers-reduced-motion: reduce) {
  .tech-item {
    transition: none;
    opacity: 1;
    transform: none;
  }
}
</style>
