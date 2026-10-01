<script setup lang="ts">
import { onMounted, ref } from 'vue'

import FrontLayout from '@/components/layout/FrontLayout.vue'
import TechBarChart from '@/components/tech/TechBarChart.vue'
import TechItemCard from '@/components/tech/TechItemCard.vue'
import TechPieChart from '@/components/tech/TechPieChart.vue'
import { useInView } from '@/composables/useInView'
import { fetchTechStack, type TechItem } from '@/api/tech'

/**
 * /tech 技术栈页（Spec 02）：饼图（分类占比）+ 条状图（单项熟练度）+ 技术项卡片。
 * 图表滚动进视口触发渐入；数据全部来自数据库，后台增删改后刷新即见。
 */
const items = ref<TechItem[]>([])
const loading = ref(true)
const errorMsg = ref('')

const listRef = ref<HTMLElement | null>(null)
const listVisible = useInView(listRef)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    items.value = await fetchTechStack()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <FrontLayout>
    <section class="tech-page">
      <header class="page-head">
        <h1 class="title">TECH STACK</h1>
        <p class="sub">$ tech-stack --charts · 饼图看技术版图分布，条状图看熟练深度</p>
      </header>

      <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>

      <div v-if="loading" class="loading">
        <span class="pixel-bar" aria-hidden="true"><i></i></span>
        <span>loading skills ...</span>
      </div>

      <template v-else-if="items.length">
        <div class="charts">
          <div class="panel hud-wrap">
            <div class="hud-frame" aria-hidden="true"></div>
            <h2 class="panel-title">
              <span class="pix">[A]</span> CATEGORY DISTRIBUTION
            </h2>
            <TechPieChart :items="items" />
          </div>
          <div class="panel hud-wrap">
            <div class="hud-frame" aria-hidden="true"></div>
            <h2 class="panel-title">
              <span class="pix">[B]</span> PROFICIENCY DEPTH
            </h2>
            <TechBarChart :items="items" />
          </div>
        </div>

        <div ref="listRef" class="list-panel">
          <h2 class="panel-title list-title">
            <span class="pix">[C]</span> INDEX
            <span class="count">{{ items.length }} TECHS</span>
          </h2>
          <ul class="tech-list">
            <TechItemCard
              v-for="(item, index) in items"
              :key="item.id"
              :item="item"
              :visible="listVisible"
              :index="index"
            />
          </ul>
        </div>
      </template>

      <p v-else class="empty">$ no tech items yet — 请到后台录入</p>
    </section>
  </FrontLayout>
</template>

<style scoped>
.tech-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
  max-width: 1120px;
  margin: 0 auto;
  padding: 44px 20px 64px;
}

.page-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  margin: 0;
  font-family: var(--font-pixel);
  font-size: 18px;
  font-weight: 400;
  letter-spacing: 3px;
  color: var(--green);
  text-shadow: 0 0 12px var(--green-glow);
}

.sub {
  margin: 0;
  color: var(--text-dim);
  font-size: 13px;
}

.error,
.loading,
.empty {
  margin: 0;
}

.error {
  color: var(--error);
}

.loading {
  display: flex;
  align-items: center;
  gap: 14px;
  color: var(--text-dim);
}

.charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.panel {
  position: relative;
  padding: 22px;
  background: var(--bg-panel);
  border: 1px solid var(--border);
}

.hud-wrap {
  min-width: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  font-family: var(--font-term);
  font-size: 22px;
  font-weight: 400;
  color: var(--cyan);
  letter-spacing: 1px;
}

.pix {
  font-family: var(--font-pixel);
  font-size: 11px;
  color: var(--amber);
}

.list-panel {
  padding: 0 4px;
}

.list-title {
  margin-bottom: 14px;
}

.count {
  margin-left: auto;
  color: var(--text-dim);
  font-family: var(--font-mono);
  font-size: 12px;
}

.tech-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

@media (max-width: 860px) {
  .charts {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .title,
  .pix {
    text-shadow: none;
  }
}
</style>