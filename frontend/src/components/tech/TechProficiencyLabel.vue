<script setup lang="ts">
import { computed } from 'vue'

import type { Proficiency } from '@/api/tech'
import { PROFICIENCY_LABELS } from '@/api/tech'

/**
 * 熟练度标签：精通/熟练/了解，带对应霓虹色（主绿/青/琥珀）。
 * 复用点：技术项卡片、条状图旁说明。色调值仅引用设计系统 token。
 */
const props = defineProps<{ proficiency: Proficiency }>()

const label = computed(() => PROFICIENCY_LABELS[props.proficiency] ?? '未知')
const tone = computed(() => {
  if (props.proficiency === 'master') return 'master'
  if (props.proficiency === 'proficient') return 'proficient'
  return 'familiar'
})
</script>

<template>
  <span class="proficiency-label" :data-proficiency="proficiency" :class="`--${tone}`">
    <span class="dot" aria-hidden="true"></span>
    {{ label }}
  </span>
</template>

<style scoped>
.proficiency-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border: 1px solid var(--border-bright);
  background: var(--bg-panel);
  font-size: 12px;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.dot {
  width: 6px;
  height: 6px;
  background: currentColor;
  box-shadow: 0 0 8px currentColor;
}

.--master {
  color: var(--green);
}

.--proficient {
  color: var(--cyan);
}

.--familiar {
  color: var(--amber);
}
</style>