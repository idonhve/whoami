<script setup lang="ts">
import { onMounted, ref } from 'vue'

import FrontLayout from '@/components/layout/FrontLayout.vue'
import TimelineMain from '@/components/experience/TimelineMain.vue'
import { fetchExperiences, type Experience } from '@/api/experience'

/**
 * 工作经历页（Spec 09）：滚动点亮时间轴 + 战果视觉卡片。
 * 数据来自 experience 表（公开接口），后台维护即前台刷新见。
 */
const experiences = ref<Experience[]>([])
const loading = ref(true)
const failed = ref(false)

async function load() {
  loading.value = true
  failed.value = false
  try {
    experiences.value = await fetchExperiences()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <FrontLayout>
    <section class="experience-page" aria-label="工作经历">
      <header class="page-head">
        <p class="prompt">$ cat ~/experience --timeline --visual</p>
        <h1 class="title">EXPERIENCE</h1>
        <p class="sub">
          <span v-if="loading">reading career log ...</span>
          <template v-else-if="failed">[error] 暂时无法读取经历数据</template>
          <template v-else-if="experiences.length === 0">no records · 等待录入经历</template>
          <template v-else>timeline · {{ experiences.length }} records</template>
        </p>
      </header>

      <div v-if="!loading && experiences.length > 0" class="timeline-wrap">
        <TimelineMain :experiences="experiences" />
      </div>

      <div v-else-if="!loading && !failed" class="empty-state">
        <div class="hud-frame" aria-hidden="true"></div>
        <p class="empty-line">$ whoami --career</p>
        <p class="empty-line dim">经历数据尚未录入，稍后再来 ——</p>
      </div>
    </section>
  </FrontLayout>
</template>

<style scoped>
.experience-page {
  position: relative;
  width: min(960px, 92vw);
  margin: 0 auto;
  padding: 48px 0 64px;
}

.page-head {
  margin-bottom: 28px;
}

.prompt {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 20px;
}

.title {
  margin: 0;
  font-family: var(--font-term);
  font-size: clamp(36px, 6vw, 56px);
  color: var(--green);
  text-shadow:
    0 0 10px var(--green-glow),
    0 0 36px var(--green-glow);
}

.sub {
  margin: 4px 0 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 19px;
}

.timeline-wrap {
  position: relative;
}

.empty-state {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 56px 24px;
  border: 2px solid var(--border);
  background: var(--bg-panel);
}

.empty-line {
  margin: 0;
  font-family: var(--font-term);
  font-size: 20px;
  color: var(--text);
}

.empty-line.dim {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-dim);
}
</style>
