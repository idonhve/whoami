<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import FrontLayout from '@/components/layout/FrontLayout.vue'
import ResumeDownloadButton from '@/components/resume/DownloadButton.vue'
import { shouldPlayBoot } from '@/composables/bootSession'
import { resolveDegraded } from '@/composables/degrade'
import { useSiteStore } from '@/stores/site'
import BootAnimation from '@/views/boot/BootAnimation.vue'
import FeaturedWorks from '@/views/home/FeaturedWorks.vue'
import HeroSection from '@/views/home/HeroSection.vue'

/**
 * 首页（F1）：开机动画 + Hero + 占位区块骨架。
 * 公共文件约定：Spec 04 填充「精选作品」、Spec 07 填充「简历下载」，在其上追加不重建。
 * Spec 07（F7）：简历下载区块已填充，按钮实现见 components/resume/DownloadButton.vue，
 * 未上传简历时按钮自动隐藏（本区块保留标题与提示，不报错）。
 */

const site = useSiteStore()

// 同步决策是否播开机动画，避免首帧闪白 / 闪主页
const showBoot = ref(shouldPlayBoot())

// 降级判定跟随站点配置（degrade_force_full 强制满血）
const degraded = computed(() => resolveDegraded(site.config.degradeForceFull))

function onBootFinished() {
  showBoot.value = false
}

onMounted(() => {
  void site.load()
})
</script>

<template>
  <FrontLayout>
    <HeroSection :owner-name="site.config.ownerName" :degraded="degraded" :active="!showBoot" />

    <!-- Spec 04「精选作品」已填充 / Spec 07「简历下载」已填充 -->
    <slot name="featured-works">
      <FeaturedWorks />
    </slot>
    <slot name="resume-download">
      <section class="resume-slot" aria-label="简历下载">
        <header class="slot-head">
          <span class="slot-cmd">$ wget ~/resume.pdf</span>
          <span class="slot-tag">SPEC-07 // ONLINE</span>
        </header>
        <div class="slot-body">
          <p class="slot-line dim">最新简历 PDF · 点击即下载（未上传时按钮自动隐藏）</p>
          <ResumeDownloadButton />
        </div>
      </section>
    </slot>
  </FrontLayout>

  <!-- 首次进入站点的开机动画（与全局路由过场 vt-* 相互独立） -->
  <BootAnimation v-if="showBoot" :degraded="degraded" @finished="onBootFinished" />
</template>

<style scoped>
/* Spec 07「简历下载」区块（样式口径与原 F1 占位一致，只引 token） */
.resume-slot {
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
  color: var(--green);
}

.slot-body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;
  padding: 28px 20px;
}

.slot-line {
  margin: 0;
  font-size: 14px;
}

.slot-line.dim {
  color: var(--text-dim);
}
</style>
