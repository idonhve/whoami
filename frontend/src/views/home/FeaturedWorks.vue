<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import RepoCard from '@/components/works/RepoCard.vue'
import { fetchWorks, type WorkCard } from '@/api/works'

/**
 * 首页「精选作品」区（Spec 04 填充 Spec 01 预留 slot）：
 * 仅置顶项（≤ 3），卡片组件与 /works 复用，避免两处实现漂移。
 */
const featured = ref<WorkCard[]>([])
const failed = ref(false)

onMounted(async () => {
  try {
    featured.value = await fetchWorks('featured')
  } catch {
    failed.value = true
  }
})
</script>

<template>
  <section class="home-featured" aria-label="精选作品">
    <header class="slot-head">
      <span class="slot-cmd">$ ls ~/featured</span>
      <span class="slot-tag">PINNED &lt;= 3</span>
    </header>

    <div class="slot-body">
      <div v-if="featured.length > 0" class="featured-grid">
        <RepoCard
          v-for="(work, index) in featured"
          :key="work.id"
          :work="work"
          :style="{ '--card-delay': `${index * 80}ms` }"
        />
      </div>

      <p v-else-if="failed" class="slot-line dim">精选作品暂不可用，稍后再试 ——</p>

      <template v-else>
        <p class="slot-line dim">暂未置顶精选作品</p>
        <RouterLink class="more-link" to="/works">cd /works &gt;</RouterLink>
      </template>
    </div>
  </section>
</template>

<style scoped>
.home-featured {
  width: min(960px, 92vw);
  margin: 0 auto 64px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
}

.slot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border);
}

.slot-cmd {
  font-family: var(--font-term);
  font-size: 18px;
  color: var(--green);
  text-shadow: 0 0 6px var(--green-glow);
}

.slot-tag {
  font-family: var(--font-pixel);
  font-size: 8px;
  letter-spacing: 1px;
  color: var(--amber);
}

.slot-body {
  padding: 20px;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.slot-line {
  margin: 0;
  font-size: 14px;
}

.slot-line.dim {
  color: var(--text-dim);
}

.more-link {
  display: inline-block;
  margin-top: 8px;
  font-family: var(--font-term);
  font-size: 18px;
  color: var(--green);
  text-shadow: 0 0 6px var(--green-glow);
}

.more-link:hover {
  text-shadow:
    1px 0 var(--magenta),
    -1px 0 var(--cyan),
    0 0 10px var(--green-glow);
}
</style>
