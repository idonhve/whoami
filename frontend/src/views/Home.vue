<script setup lang="ts">
// M1 地基占位页：正式前台布局与 Hero 由 Spec 01 接管
</script>

<template>
  <main class="home-placeholder">
    <p class="prompt">$ whoami --welcome</p>
    <p class="hint">前台页面建设中（Spec 01：欢迎页 + 首页 Hero）。</p>
    <p class="hint">管理后台入口：<RouterLink to="/admin/login">/admin/login</RouterLink></p>
  </main>
</template>

<style scoped>
.home-placeholder {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.prompt {
  color: var(--accent);
  font-size: 18px;
  margin: 0;
}

.hint {
  color: var(--text-dim);
  margin: 0;
}
</style>
