<script setup lang="ts">
import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'

/**
 * 前台页面骨架：页头（命令式导航）+ 内容 + 页脚。
 * 各前台页面（首页 / 作品 / 技术栈 / 经历 / 证书 / 关于）统一包一层。
 */
</script>

<template>
  <div class="front-layout">
    <AppHeader />
    <main class="front-main">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
.front-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.front-main {
  flex: 1;
  /* 为 fixed 页头留出空间 */
  padding-top: 56px;
}
</style>
