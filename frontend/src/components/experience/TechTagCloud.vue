<script setup lang="ts">
import { computed } from 'vue'

import { iconPath } from '@/components/tech/iconPaths'

/**
 * 技术标签图标云（Spec 09）：technical tag 以图标云呈现。
 * 能命中 devicon 映射（iconPaths）时画 SVG 图标 + 标签名；未命中的渲染中性文字占位（避免 emoji）。
 */
const props = defineProps<{ techTags: string[] }>()

const tags = computed(() =>
  props.techTags.map((t, index) => ({
    id: `${index}-${t}`,
    label: t.trim(),
    path: iconPath(t),
  })),
)
</script>

<template>
  <ul class="tag-cloud" aria-label="技术标签" data-testid="tech-tag-cloud">
    <li v-for="tag in tags" :key="tag.id" class="tag">
      <svg
        v-if="tag.path"
        class="tag-icon"
        viewBox="0 0 24 24"
        aria-hidden="true"
        :data-known="tag.label"
      >
        <path :d="tag.path" fill="currentColor" />
      </svg>
      <span
        v-else
        class="tag-icon tag-icon--fallback"
        aria-hidden="true"
        :data-fallback="tag.label"
      >
        {{ tag.label.slice(0, 2) }}
      </span>
      <span class="tag-name">{{ tag.label }}</span>
    </li>
  </ul>
</template>

<style scoped>
.tag-cloud {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
  color: var(--text);
}

.tag-icon {
  width: 20px;
  height: 20px;
  color: var(--cyan);
  filter: drop-shadow(0 0 6px var(--cyan-soft));
  flex: none;
}

.tag-icon--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-pixel);
  font-size: 9px;
  border: 1px solid var(--border-bright);
  color: var(--text-dim);
  width: 22px;
  height: 22px;
}

.tag-name {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-dim);
}
</style>
