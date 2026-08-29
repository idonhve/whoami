<script setup lang="ts">
import { darkTheme, dateZhCN, NConfigProvider, zhCN } from 'naive-ui'

import RouteTransitionOverlay from '@/components/RouteTransitionOverlay.vue'
import { routeTransition } from '@/composables/routeTransition'
</script>

<template>
  <NConfigProvider :theme="darkTheme" :locale="zhCN" :date-locale="dateZhCN">
    <div class="view-root" :class="`vt-${routeTransition.phase}`">
      <RouterView />
    </div>
  </NConfigProvider>
  <RouteTransitionOverlay />
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

/* ---------- 路由过场：CRT 断电 / 通电 ---------- */

.view-root {
  transform-origin: 50% 50%;
  min-height: 100vh;
}

/* 断电：画面向中心压扁成一条亮线，亮度飙升后熄灭 */
.view-root.vt-collapse {
  animation: vt-crt-off 0.46s cubic-bezier(0.55, 0, 0.85, 0.36) both;
}

.view-root.vt-boot {
  opacity: 0;
}

/* 通电：从一条亮线展开，过冲回弹 + 亮度闪白 */
.view-root.vt-reveal {
  animation: vt-crt-on 0.52s cubic-bezier(0.23, 1, 0.32, 1) both;
}

@keyframes vt-crt-off {
  0% {
    transform: none;
    filter: none;
    opacity: 1;
  }
  45% {
    transform: scaleY(0.02) scaleX(0.94);
    filter: brightness(1.7) saturate(1.5);
    opacity: 1;
  }
  70% {
    transform: scaleY(0.008) scaleX(0.55);
    filter: brightness(3.2);
  }
  100% {
    transform: scaleY(0.002) scaleX(0.2);
    filter: brightness(5);
    opacity: 0;
  }
}

@keyframes vt-crt-on {
  0% {
    transform: scaleY(0.004) scaleX(0.35);
    filter: brightness(4.5);
    opacity: 1;
  }
  35% {
    transform: scaleY(1.03) scaleX(1);
    filter: brightness(1.9);
    opacity: 1;
  }
  55% {
    transform: scaleY(0.99);
    filter: brightness(1.1);
  }
  72% {
    transform: scaleY(1.005);
  }
  100% {
    transform: none;
    filter: none;
    opacity: 1;
  }
}
</style>
