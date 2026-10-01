<script setup lang="ts">
import { onMounted, ref } from 'vue'

import FrontLayout from '@/components/layout/FrontLayout.vue'
import RepoCard from '@/components/works/RepoCard.vue'
import { fetchWorks, type WorkCard } from '@/api/works'

/**
 * 作品页（Spec 04）：全部未隐藏作品卡片列表（置顶优先）。
 * 数据来自后端缓存（每日 03:00 同步 + 手动同步），GitHub 故障不影响本页。
 */
const works = ref<WorkCard[]>([])
const loading = ref(true)
const failed = ref(false)

async function load() {
  loading.value = true
  failed.value = false
  try {
    works.value = await fetchWorks('all')
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
    <section class="works-page" aria-label="作品列表">
      <header class="page-head">
        <p class="prompt">$ ls ~/works --sort=pin,update</p>
        <h1 class="title">WORKS</h1>
        <p class="sub">
          <span v-if="loading">fetching repos ...</span>
          <template v-else-if="failed">[error] 暂时无法读取作品数据</template>
          <template v-else-if="works.length === 0">0 repos · 等待首次同步</template>
          <template v-else>{{ works.length }} repos · data synced from github</template>
        </p>
      </header>

      <div v-if="!loading && works.length > 0" class="works-grid">
        <RepoCard
          v-for="(work, index) in works"
          :key="work.id"
          :work="work"
          :style="{ '--card-delay': `${Math.min(index, 8) * 60}ms` }"
        />
      </div>

      <div v-else-if="!loading && !failed" class="empty-state">
        <div class="hud-frame" aria-hidden="true"></div>
        <p class="empty-line">$ git remote -v</p>
        <p class="empty-line dim">作品数据尚未同步，稍后再来 ——</p>
      </div>
    </section>
  </FrontLayout>
</template>

<style scoped>
.works-page {
  position: relative;
  width: min(1080px, 92vw);
  margin: 0 auto;
  padding: 48px 0 64px;
}

.page-head {
  margin-bottom: 24px;
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

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
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
