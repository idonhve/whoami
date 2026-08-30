<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import FrontLayout from '@/components/layout/FrontLayout.vue'

/**
 * 前台占位页：入口命令可达，内容由对应 Spec 模块后续填充（不重建本页路由，替换 component 即可）。
 * 路由 meta: { title: string, spec: string }
 */

const route = useRoute()
const title = computed(() => (route.meta.title as string) ?? route.path)
const spec = computed(() => (route.meta.spec as string) ?? '')
</script>

<template>
  <FrontLayout>
    <section class="placeholder">
      <div class="hud-frame" aria-hidden="true"></div>
      <p class="ph-prompt">$ mount {{ route.path }}</p>
      <h1 class="ph-title">{{ title }}</h1>
      <p class="ph-line">module <span class="ph-accent">{{ spec }}</span> ........ [ PENDING ]</p>
      <p class="ph-line dim">该模块由后续 Spec 填充，导航与布局已就绪。</p>
      <RouterLink class="neon-btn" to="/">cd ~ &gt;</RouterLink>
    </section>
  </FrontLayout>
</template>

<style scoped>
.placeholder {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 20px;
  min-height: calc(100vh - 56px - 85px);
  padding: 48px 20px;
  text-align: center;
}

.ph-prompt {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 22px;
}

.ph-title {
  margin: 0;
  font-family: var(--font-term);
  font-size: clamp(36px, 6vw, 64px);
  color: var(--green);
  text-shadow:
    0 0 10px var(--green-glow),
    0 0 36px var(--green-glow);
}

.ph-line {
  margin: 0;
  font-family: var(--font-term);
  font-size: 19px;
  color: var(--text);
}

.ph-line.dim {
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--text-dim);
}

.ph-accent {
  color: var(--amber);
}
</style>
