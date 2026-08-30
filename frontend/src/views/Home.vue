<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import FrontLayout from '@/components/layout/FrontLayout.vue'
import { shouldPlayBoot } from '@/composables/bootSession'
import { resolveDegraded } from '@/composables/degrade'
import { useSiteStore } from '@/stores/site'
import BootAnimation from '@/views/boot/BootAnimation.vue'
import FeaturedWorksPlaceholder from '@/views/home/FeaturedWorksPlaceholder.vue'
import HeroSection from '@/views/home/HeroSection.vue'
import ResumeDownloadPlaceholder from '@/views/home/ResumeDownloadPlaceholder.vue'

/**
 * 首页（F1）：开机动画 + Hero + 占位区块骨架。
 * 公共文件约定：Spec 04 填充「精选作品」、Spec 07 填充「简历下载」，在其上追加不重建。
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

    <!-- Spec 04 填充「精选作品」/ Spec 07 填充「简历下载」 -->
    <slot name="featured-works">
      <FeaturedWorksPlaceholder />
    </slot>
    <slot name="resume-download">
      <ResumeDownloadPlaceholder />
    </slot>
  </FrontLayout>

  <!-- 首次进入站点的开机动画（与全局路由过场 vt-* 相互独立） -->
  <BootAnimation v-if="showBoot" :degraded="degraded" @finished="onBootFinished" />
</template>
