<script setup lang="ts">
import { routeTransition } from '@/composables/routeTransition'
</script>

<template>
  <div
    v-if="routeTransition.phase !== 'idle'"
    class="vt-overlay"
    :class="`is-${routeTransition.phase}`"
    aria-hidden="true"
  >
    <!-- 阶段 2：终端引导日志 -->
    <div v-if="routeTransition.phase === 'boot'" class="vt-boot">
      <p class="vt-line vt-l1">$ mount {{ routeTransition.target }}</p>
      <p class="vt-line vt-l2">&nbsp;&nbsp;resolving module ......... [ OK ]</p>
      <p class="vt-line vt-l3">&nbsp;&nbsp;linking render pipeline .. [ OK ]</p>
      <p class="vt-line vt-l4">
        &nbsp;&nbsp;<span class="vt-bar"><i></i></span> 100%
      </p>
      <p class="vt-line vt-l5">&nbsp;&nbsp;0x7F3A mem ............... [ OK ]</p>
    </div>

    <!-- 阶段 3：通电瞬间的白闪 + 下扫光束 + 像素故障条 -->
    <template v-if="routeTransition.phase === 'reveal'">
      <div class="vt-flash"></div>
      <div class="vt-beam"></div>
      <div class="vt-slice s1"></div>
      <div class="vt-slice s2"></div>
      <div class="vt-slice s3"></div>
    </template>
  </div>
</template>

<style scoped>
.vt-overlay {
  position: fixed;
  inset: 0;
  z-index: 9900;
  pointer-events: auto;
  background: transparent;
}

.vt-overlay.is-boot {
  background: #020407;
}

/* ---------- 终端引导日志 ---------- */

.vt-boot {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: max-content;
  max-width: 86vw;
  font-family: var(--font-term);
  font-size: 22px;
  letter-spacing: 0.5px;
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
}

.vt-line {
  margin: 0;
  white-space: nowrap;
  opacity: 0;
  animation: vt-line-in 0.01s steps(1) forwards;
}

.vt-l1 {
  animation-delay: 0.02s;
}

.vt-l2 {
  animation-delay: 0.14s;
  color: var(--text-dim);
  text-shadow: none;
}

.vt-l3 {
  animation-delay: 0.26s;
  color: var(--text-dim);
  text-shadow: none;
}

.vt-l4 {
  animation-delay: 0.38s;
}

.vt-l5 {
  animation-delay: 0.5s;
  color: var(--text-dim);
  text-shadow: none;
}

.vt-l5::after {
  content: '▌';
  color: var(--green);
  animation: cursor-blink 0.6s steps(1) infinite;
}

@keyframes vt-line-in {
  to {
    opacity: 1;
  }
}

/* 像素进度条：steps 逐格推进，硬核像素感 */
.vt-bar {
  display: inline-block;
  width: 196px;
  height: 14px;
  border: 2px solid var(--green);
  padding: 1px;
  vertical-align: -2px;
  box-shadow: 0 0 10px rgba(0, 255, 156, 0.25);
}

.vt-bar > i {
  display: block;
  height: 100%;
  width: 100%;
  background: repeating-linear-gradient(
    90deg,
    var(--green) 0 10px,
    transparent 10px 14px
  );
  transform-origin: left;
  animation: vt-bar-fill 0.22s steps(14) both;
  animation-delay: 0.38s;
}

@keyframes vt-bar-fill {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

/* ---------- 通电瞬间 ---------- */

.vt-flash {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center,
    rgba(210, 255, 236, 0.55) 0%,
    rgba(0, 255, 156, 0.18) 45%,
    transparent 75%
  );
  animation: vt-flash-out 0.18s ease-out both;
}

@keyframes vt-flash-out {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

/* 扫描光束自上而下扫过 */
.vt-beam {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 4px;
  background: linear-gradient(
    180deg,
    rgba(0, 255, 156, 0),
    var(--green) 60%,
    #d8fff0
  );
  box-shadow:
    0 0 18px var(--green-glow),
    0 0 46px rgba(0, 255, 156, 0.3);
  animation: vt-beam-sweep 0.52s cubic-bezier(0.3, 0.6, 0.4, 1) both;
}

@keyframes vt-beam-sweep {
  from {
    transform: translateY(-8vh);
    opacity: 1;
  }
  85% {
    opacity: 1;
  }
  to {
    transform: translateY(108vh);
    opacity: 0;
  }
}

/* 像素故障条：steps(1) 硬切抖动 */
.vt-slice {
  position: absolute;
  left: 0;
  right: 0;
  mix-blend-mode: screen;
  opacity: 0;
  animation: vt-slice-jitter 0.4s steps(1) both;
}

.vt-slice.s1 {
  top: 22%;
  height: 12px;
  background: rgba(255, 46, 136, 0.35);
}

.vt-slice.s2 {
  top: 55%;
  height: 18px;
  background: rgba(43, 217, 255, 0.3);
  animation-delay: 0.06s;
}

.vt-slice.s3 {
  top: 78%;
  height: 9px;
  background: rgba(0, 255, 156, 0.3);
  animation-delay: 0.12s;
}

@keyframes vt-slice-jitter {
  0% {
    opacity: 0;
  }
  12% {
    opacity: 1;
    transform: translateX(-14px);
  }
  24% {
    transform: translateX(9px);
  }
  36% {
    opacity: 0;
  }
  52% {
    opacity: 1;
    transform: translateX(5px);
  }
  64% {
    transform: translateX(-7px);
    opacity: 0.7;
  }
  78%,
  100% {
    opacity: 0;
  }
}
</style>
