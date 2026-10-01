<script setup lang="ts">
import { computed, defineAsyncComponent, onBeforeUnmount, ref, watch } from 'vue'

import { prefersReducedMotion } from '@/utils/motion'
import { HERO_COMMANDS, HERO_TAGLINES } from '@/views/home/heroContent'

/**
 * 首页 Hero：姓名 + 一句话定位（打字机）+ 3D 锚点 + 入口命令。
 * 3D 场景异步懒加载（独立 three 分包，不阻塞首包红线）；
 * 降级模式 / reduced-motion 下用静态渐变背景替代。
 */

const props = defineProps<{
  ownerName: string
  degraded: boolean
  /** 开机动画结束后才开始 Hero 动效，避免与过渡层抢帧 */
  active: boolean
}>()

// 仅在满血版懒加载 3D 锚点（降级路径永不触发 three 分包下载）；
// degraded 随站点配置（degrade_force_full）异步更新，用 computed 跟随
const HeroScene3D = computed(() =>
  !props.degraded && !prefersReducedMotion()
    ? defineAsyncComponent(() => import('@/views/home/HeroScene3D.vue'))
    : null,
)

const typed = ref('')
let timer: number | undefined
let phraseIdx = 0
let charIdx = 0
let deleting = false

function tick() {
  const phrase = HERO_TAGLINES[phraseIdx]
  if (!deleting) {
    charIdx += 1
    typed.value = phrase.slice(0, charIdx)
    if (charIdx === phrase.length) {
      deleting = true
      timer = window.setTimeout(tick, 2400)
      return
    }
  } else {
    charIdx -= 1
    typed.value = phrase.slice(0, charIdx)
    if (charIdx === 0) {
      deleting = false
      phraseIdx = (phraseIdx + 1) % HERO_TAGLINES.length
    }
  }
  timer = window.setTimeout(tick, deleting ? 24 : 95)
}

let started = false
function startTypewriter() {
  if (started) return
  started = true
  if (prefersReducedMotion()) {
    typed.value = HERO_TAGLINES[0]
    return
  }
  tick()
}

// 等开机动画交接完成再启动打字机（无开机动画时 active 初始即为 true）
watch(
  () => props.active,
  (active) => {
    if (active) startTypewriter()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (timer !== undefined) window.clearTimeout(timer)
})
</script>

<template>
  <section class="hero" aria-label="首页 Hero">
    <!-- 3D 锚点（满血版懒加载） / 降级静态渐变背景 -->
    <HeroScene3D v-if="HeroScene3D" />
    <div v-else class="hero-static-bg" aria-hidden="true"></div>

    <div class="hud-frame" aria-hidden="true"></div>

    <div class="hero-inner">
      <p class="hero-prompt">$ whoami --verbose</p>
      <h1 class="hero-name" :data-text="ownerName">{{ ownerName }}</h1>
      <p class="hero-tagline">
        <span>{{ typed }}</span><span class="hero-cursor" aria-hidden="true">▌</span>
      </p>

      <nav class="hero-cmds" aria-label="入口命令">
        <RouterLink v-for="cmd in HERO_COMMANDS" :key="cmd.to" :to="cmd.to" class="hero-cmd">
          <span class="hero-cmd-label"
            ><span class="cmd-dollar" aria-hidden="true">$</span> {{ cmd.label }}</span
          >
          <span class="hero-cmd-desc"># {{ cmd.desc }}</span>
        </RouterLink>
      </nav>
    </div>

    <p class="hero-scroll" aria-hidden="true">
      <svg width="10" height="8" viewBox="0 0 10 8" fill="currentColor" aria-hidden="true">
        <path d="M5 8 0 0h10z" />
      </svg>
      SCROLL
    </p>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 56px);
  padding: 48px 20px;
  overflow: hidden;
  text-align: center;
}

/* 降级 / reduced-motion 的静态渐变背景（替代 3D 锚点） */
.hero-static-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 55% 38% at 50% 42%, var(--green-soft), transparent 70%),
    radial-gradient(ellipse 40% 30% at 78% 68%, var(--cyan-soft), transparent 70%),
    radial-gradient(circle, var(--cyan-soft) 1px, transparent 1px);
  background-size:
    100% 100%,
    100% 100%,
    30px 30px;
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  max-width: 760px;
}

.hero-prompt {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 22px;
  animation: rise-in 0.5s ease-out both;
}

.hero-name {
  position: relative;
  margin: 0;
  font-family: var(--font-term);
  font-size: clamp(44px, 9vw, 88px);
  line-height: 1.1;
  letter-spacing: 2px;
  color: var(--green);
  text-shadow:
    0 0 10px var(--green-glow),
    0 0 40px var(--green-glow);
  animation: rise-in 0.5s ease-out 0.08s both;
}

/* 故障闪切：双层错位剪影周期性闪现（steps 硬切） */
.hero-name::before,
.hero-name::after {
  content: attr(data-text);
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}

.hero-name::before {
  color: var(--cyan);
  animation: hero-glitch-a 7s steps(1) infinite;
}

.hero-name::after {
  color: var(--magenta);
  animation: hero-glitch-b 7s steps(1) infinite;
}

@keyframes hero-glitch-a {
  0%,
  91% {
    opacity: 0;
  }
  91.5% {
    opacity: 0.8;
    clip-path: inset(12% 0 74% 0);
    transform: translateX(-5px);
  }
  92.5% {
    clip-path: inset(60% 0 24% 0);
    transform: translateX(4px);
  }
  93.5% {
    opacity: 0;
  }
}

@keyframes hero-glitch-b {
  0%,
  91% {
    opacity: 0;
  }
  91.5% {
    opacity: 0.8;
    clip-path: inset(56% 0 28% 0);
    transform: translateX(5px);
  }
  92.5% {
    clip-path: inset(6% 0 82% 0);
    transform: translateX(-4px);
  }
  93.5% {
    opacity: 0;
  }
}

.hero-tagline {
  margin: 0;
  min-height: 24px;
  color: var(--text);
  font-family: var(--font-mono);
  font-size: clamp(14px, 2vw, 17px);
  animation: rise-in 0.5s ease-out 0.16s both;
}

.hero-cursor {
  display: inline-block;
  color: var(--green);
  animation: cursor-blink 1s steps(1) infinite;
}

.hero-cmds {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: min(460px, 100%);
  margin-top: 8px;
  text-align: left;
  animation: rise-in 0.5s ease-out 0.24s both;
}

.hero-cmd {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  min-height: 44px;
  padding: 8px 16px;
  border-left: 2px solid var(--border-bright);
  background: color-mix(in srgb, var(--bg-panel) 60%, transparent);
  transition:
    border-color 0.2s,
    background 0.2s,
    transform 0.15s;
}

.hero-cmd:hover {
  border-left-color: var(--green);
  background: var(--green-soft);
  transform: translateX(4px);
}

.hero-cmd-label {
  font-family: var(--font-term);
  font-size: 19px;
  color: var(--green);
  text-shadow: 0 0 6px var(--green-glow);
  white-space: nowrap;
}

.cmd-dollar {
  color: var(--magenta);
}

.hero-cmd-desc {
  color: var(--text-dim);
  font-size: 13px;
  white-space: nowrap;
}

.hero-scroll {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-pixel);
  font-size: 9px;
  letter-spacing: 2px;
  animation: led-pulse 1.8s steps(2) infinite;
}

@media (prefers-reduced-motion: reduce) {
  .hero-prompt,
  .hero-name,
  .hero-tagline,
  .hero-cmds {
    animation: none;
  }

  .hero-name::before,
  .hero-name::after,
  .hero-cursor,
  .hero-scroll {
    animation: none;
  }

  .hero-cmd:hover {
    transform: none;
  }
}
</style>
