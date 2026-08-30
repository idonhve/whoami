<script setup lang="ts">
/**
 * 全局前台页脚。
 * 后续模块挂载点：Spec 03 在 .footer-side 追加 GitHub 图标。
 */
const year = new Date().getFullYear()
</script>

<template>
  <footer class="app-footer">
    <span class="footer-item">© {{ year }} whoami</span>
    <span class="footer-item footer-mid">EST. 2026 // TERMINAL × PIXEL × NEON</span>
    <!-- Spec 03 在此追加 GitHub 图标 -->
    <span class="footer-side"></span>
  </footer>
</template>

<style scoped>
.app-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-top: 1px solid var(--border);
  color: var(--text-dim);
  font-size: 11px;
  letter-spacing: 1px;
}

.footer-item {
  white-space: nowrap;
}

.footer-side {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
}

@media (max-width: 560px) {
  .footer-mid {
    display: none;
  }
}
</style>
