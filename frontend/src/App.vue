<script setup lang="ts">
import { darkTheme, dateZhCN, NConfigProvider, zhCN } from 'naive-ui'
</script>

<template>
  <NConfigProvider :theme="darkTheme" :locale="zhCN" :date-locale="dateZhCN">
    <RouterView />
  </NConfigProvider>
  <div class="crt-overlay" aria-hidden="true"></div>
</template>

<style>
/* 全局 CRT 质感层：扫描线 + 暗角 + 缓慢滚动的亮带。
   pointer-events: none，不影响任何交互；reduced-motion 时静止。 */
.crt-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
  background: repeating-linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.16) 0 1px,
    transparent 1px 3px
  );
  animation: crt-flicker 7s linear infinite;
}

.crt-overlay::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(255, 255, 255, 0.018) 7%,
    transparent 15%
  );
  animation: crt-roll 9s linear infinite;
}

.crt-overlay::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center,
    transparent 58%,
    rgba(0, 0, 0, 0.32) 100%
  );
}

@keyframes crt-roll {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(100%);
  }
}

@keyframes crt-flicker {
  0%,
  100% {
    opacity: 1;
  }
  96% {
    opacity: 1;
  }
  96.5% {
    opacity: 0.92;
  }
  97% {
    opacity: 1;
  }
  98.5% {
    opacity: 0.96;
  }
}

@media (prefers-reduced-motion: reduce) {
  .crt-overlay {
    animation: none;
  }

  .crt-overlay::before {
    animation: none;
    display: none;
  }
}
</style>
