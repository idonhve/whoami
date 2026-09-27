<script setup lang="ts">
import { onMounted, ref } from 'vue'

import ChinaMap from '@/components/about/ChinaMap.vue'
import MessageBoard from '@/components/about/MessageBoard.vue'
import FrontLayout from '@/components/layout/FrontLayout.vue'
import ResumeDownloadButton from '@/components/resume/DownloadButton.vue'
import { fetchVisitStatsGeo, type GeoProvince } from '@/api/stats'

/**
 * 关于本站（Spec 05 /about，Spec 07 简历下载 slot 复用 DownloadButton）：
 * 访客地图（省级聚合 + 呼吸打点）→ 简历下载 → 留言板（#message-board 锚点）。
 * 地图数据来自公开 GET /api/visit-stats/geo（不含任何 IP 信息）。
 */
const provinces = ref<GeoProvince[]>([])
const loading = ref(true)
const failed = ref(false)

onMounted(async () => {
  try {
    provinces.value = await fetchVisitStatsGeo()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <FrontLayout>
    <section class="about-page" aria-label="关于本站">
      <header class="page-head">
        <p class="prompt">$ whoami --about --stats</p>
        <h1 class="title">ABOUT</h1>
        <p class="sub">
          <template v-if="loading">scanning visitor logs ...</template>
          <template v-else-if="failed">[error] 访客数据暂时无法读取（留言板仍可用）</template>
          <template v-else>
            {{ provinces.length }} 个省市的朋友来过 · 这个站正被谁看着
          </template>
        </p>
      </header>

      <!-- 访客地图：省级聚合 + 呼吸打点（本页唯一重动效锚点） -->
      <section class="block" aria-label="访客地图">
        <h2 class="block-title">VISITOR MAP</h2>
        <ChinaMap :provinces="provinces" :loading="loading" />
      </section>

      <!-- Spec 07 预留的简历下载 slot：复用全站唯一 DownloadButton 实现 -->
      <section class="block resume-block" aria-label="简历下载">
        <h2 class="block-title">RESUME</h2>
        <p class="block-line dim">觉得匹配？拿走最新简历（未上传时按钮自动隐藏）</p>
        <div class="resume-slot">
          <ResumeDownloadButton />
        </div>
      </section>

      <!-- 留言板：组件根元素带 id="message-board" 锚点 -->
      <section class="block" aria-label="留言板">
        <h2 class="block-title">MESSAGE BOARD</h2>
        <MessageBoard />
      </section>
    </section>
  </FrontLayout>
</template>

<style scoped>
.about-page {
  width: min(1080px, 92vw);
  margin: 0 auto;
  padding: 48px 0 64px;
}

.page-head {
  margin-bottom: 28px;
}

.prompt {
  margin: 0;
  font-family: var(--font-term);
  font-size: 20px;
  color: var(--text-dim);
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
  font-family: var(--font-term);
  font-size: 19px;
  color: var(--text-dim);
}

.block {
  margin-bottom: 48px;
  padding: 20px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
}

.block-title {
  margin: 0 0 16px;
  font-family: var(--font-pixel);
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 2px;
  color: var(--cyan);
  text-shadow: 0 0 8px var(--cyan-soft);
}

.block-line {
  margin: 0 0 14px;
  font-size: 13px;
}

.dim {
  color: var(--text-dim);
}

.resume-slot {
  display: flex;
  justify-content: flex-start;
  padding: 4px 0;
}
</style>
