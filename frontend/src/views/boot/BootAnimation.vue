<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { markBootSeen, loadBootMuted, saveBootMuted } from '@/composables/bootSession'
import { useSiteStore } from '@/stores/site'
import { cssVar } from '@/utils/cssVar'
import { prefersReducedMotion, wait } from '@/utils/motion'
import { buildBootLogs, type BootLogLine } from '@/views/boot/bootLogs'
import { BootSound } from '@/views/boot/bootSound'

const props = defineProps<{
  /** 降级模式：无粒子过渡，仅轻量渐隐 */
  degraded: boolean
}>()

const emit = defineEmits<{
  finished: []
}>()

type Phase = 'typing' | 'transition'

/** 总时长预算：打字 ≈1.1s + 过渡 ≈0.7s，动画全程 ≤ 2.5s（PRD §4.1）。
 *  逐字 await 会被浏览器定时器下限拖慢，按节拍批量敲出保证时长可控。 */
const CHARS_PER_TICK = 3
const TICK_MS = 10
const LINE_PAUSE_MS = 40
const REDUCED_HOLD_MS = 900
/** 粒子时长 = 淡出动画 0.38s 延迟 + 0.32s 时长，两段同时收尾避免遮罩半透明时突然消失 */
const PARTICLE_MS = 700

const site = useSiteStore()
const sound = new BootSound()

const phase = ref<Phase>('typing')
const doneLines = ref<BootLogLine[]>([])
const currentText = ref('')
const progress = ref(0)
const muted = ref(loadBootMuted())
const reduced = prefersReducedMotion()

const lines = ref<BootLogLine[]>([])
const totalChars = computed(() => lines.value.reduce((sum, l) => sum + l.text.length, 0))

let cancelled = false
let particleRaf = 0
const particleCanvas = ref<HTMLCanvasElement | null>(null)

sound.muted = muted.value

function toggleMute() {
  muted.value = !muted.value
  sound.muted = muted.value
  saveBootMuted(muted.value)
  if (!muted.value) sound.unlock()
}

function finish() {
  if (cancelled) return
  cancelled = true
  markBootSeen()
  emit('finished')
}

/** 点击 / 回车跳过 */
function skip() {
  finish()
}

async function typeSequence() {
  let typedChars = 0
  for (const line of lines.value) {
    let i = 0
    while (i < line.text.length) {
      if (cancelled) return
      i = Math.min(line.text.length, i + CHARS_PER_TICK)
      currentText.value = line.text.slice(0, i)
      typedChars = Math.min(totalChars.value, typedChars + CHARS_PER_TICK)
      progress.value = Math.min(0.96, typedChars / Math.max(1, totalChars.value))
      sound.blip()
      await wait(TICK_MS)
    }
    if (cancelled) return
    doneLines.value.push(line)
    currentText.value = ''
    sound.confirm()
    await wait(LINE_PAUSE_MS)
  }
}

/** 粒子过渡：像素方块从文本区向外散开，仅满血版播放 */
function runParticles(): Promise<void> {
  return new Promise((resolve) => {
    const canvas = particleCanvas.value
    if (!canvas) {
      resolve()
      return
    }
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      resolve()
      return
    }
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    canvas.width = window.innerWidth * dpr
    canvas.height = window.innerHeight * dpr
    ctx.scale(dpr, dpr)

    const w = window.innerWidth
    const h = window.innerHeight
    const cx = w / 2
    const cy = h / 2
    const count = 160
    // 颜色取自设计 token（Canvas 无法直接引用 CSS 变量）
    const palette = [
      cssVar('--green', '#00ff9c'),
      cssVar('--cyan', '#2bd9ff'),
      cssVar('--magenta', '#ff2e88'),
      cssVar('--text', '#c8d6e5'),
    ]
    const parts = Array.from({ length: count }, (_, i) => {
      const angle = (i / count) * Math.PI * 2 + Math.random() * 0.4
      const speed = 2.5 + Math.random() * 6
      const size = 2 + Math.floor(Math.random() * 3) * 2
      return {
        x: cx + (Math.random() - 0.5) * w * 0.4,
        y: cy + (Math.random() - 0.5) * h * 0.3,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        size,
        color: palette[i % palette.length],
      }
    })

    const start = performance.now()
    const frame = (now: number) => {
      if (cancelled) {
        resolve()
        return
      }
      const t = now - start
      const k = Math.min(1, t / PARTICLE_MS)
      ctx.clearRect(0, 0, w, h)
      ctx.globalAlpha = 1 - k
      for (const p of parts) {
        p.x += p.vx
        p.y += p.vy
        ctx.fillStyle = p.color
        ctx.fillRect(Math.round(p.x), Math.round(p.y), p.size, p.size)
      }
      if (k < 1) {
        particleRaf = requestAnimationFrame(frame)
      } else {
        resolve()
      }
    }
    particleRaf = requestAnimationFrame(frame)
  })
}

async function run() {
  // 等站点配置（域名文案），最多等 400ms，接口慢/挂时用默认值，全程无白屏
  await Promise.race([site.load(), wait(400)])
  if (cancelled) return
  lines.value = buildBootLogs(site.config.domain)

  if (reduced) {
    // 静态版：全部日志一次性呈现，短暂停留后直接进入主页
    doneLines.value = lines.value
    progress.value = 1
    await wait(REDUCED_HOLD_MS)
    finish()
    return
  }

  sound.unlock()
  await typeSequence()
  if (cancelled) return

  phase.value = 'transition'
  progress.value = 1
  if (props.degraded) {
    // 降级路径：轻量渐隐，无粒子
    await wait(320)
  } else {
    await wait(0) // 让 canvas 先挂载
    await runParticles()
  }
  finish()
}

onMounted(() => {
  void run()
})

onBeforeUnmount(() => {
  cancelled = true
  if (particleRaf) cancelAnimationFrame(particleRaf)
  sound.dispose()
})
</script>

<template>
  <div
    class="boot-overlay"
    :class="{ 'is-transition': phase === 'transition', 'is-degraded': degraded }"
    role="button"
    tabindex="0"
    aria-label="开机动画，点击跳过"
    @click="skip"
    @keydown.enter="skip"
    @keydown.space.prevent="skip"
  >
    <div class="boot-term" aria-live="polite">
      <p v-for="(line, i) in doneLines" :key="i" class="boot-line" :class="`is-${line.kind}`">
        {{ line.text }}
      </p>
      <p v-if="phase === 'typing' && !reduced" class="boot-line is-ok">
        {{ currentText }}<span class="boot-cursor" aria-hidden="true">▌</span>
      </p>
    </div>

    <canvas
      v-if="phase === 'transition' && !degraded && !reduced"
      ref="particleCanvas"
      class="boot-particles"
      aria-hidden="true"
    ></canvas>

    <div class="boot-status">
      <div class="pixel-bar boot-progress" role="progressbar" :aria-valuenow="Math.round(progress * 100)" aria-valuemin="0" aria-valuemax="100">
        <i :style="{ transform: `scaleX(${progress})` }"></i>
      </div>
      <span class="boot-pct">{{ Math.round(progress * 100) }}%</span>
      <button
        type="button"
        class="boot-mute"
        :aria-label="muted ? '取消静音' : '静音'"
        :aria-pressed="muted"
        @click.stop="toggleMute"
      >
        <svg v-if="muted" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M11 5 6 9H2v6h4l5 4V5z" />
          <line x1="23" y1="9" x2="17" y2="15" />
          <line x1="17" y1="9" x2="23" y2="15" />
        </svg>
        <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M11 5 6 9H2v6h4l5 4V5z" />
          <path d="M15.5 8.5a5 5 0 0 1 0 7" />
          <path d="M18.6 5.4a9 9 0 0 1 0 13.2" />
        </svg>
      </button>
      <span class="boot-skip-hint">CLICK TO SKIP &gt;</span>
    </div>
  </div>
</template>

<style scoped>
.boot-overlay {
  position: fixed;
  inset: 0;
  z-index: 9950;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 40px;
  padding: 24px;
  background: var(--bg);
  cursor: pointer;
}

.boot-overlay.is-transition {
  animation: boot-fade-out 0.32s cubic-bezier(0.55, 0, 0.85, 0.36) 0.38s both;
}

/* 降级路径：无粒子，直接轻量渐隐 */
.boot-overlay.is-degraded.is-transition {
  animation: boot-fade-out 0.3s cubic-bezier(0.55, 0, 0.85, 0.36) both;
}

@keyframes boot-fade-out {
  to {
    opacity: 0;
    filter: brightness(2.2);
  }
}

.boot-term {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: min(640px, 92vw);
  min-height: 220px;
  font-family: var(--font-term);
  font-size: clamp(18px, 2.6vw, 24px);
  letter-spacing: 0.5px;
}

.boot-line {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.boot-line.is-ok {
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
}

.boot-line.is-dim {
  color: var(--text-dim);
}

.boot-line.is-accent {
  color: var(--cyan);
  text-shadow: 0 0 10px var(--cyan);
}

.boot-cursor {
  display: inline-block;
  color: var(--green);
  animation: cursor-blink 0.7s steps(1) infinite;
}

.boot-particles {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.boot-status {
  display: flex;
  align-items: center;
  gap: 16px;
  width: min(640px, 92vw);
}

.boot-progress {
  flex: 1;
}

.boot-pct {
  min-width: 44px;
  color: var(--green);
  font-family: var(--font-pixel);
  font-size: 10px;
}

.boot-mute {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  padding: 8px;
  border: 2px solid var(--border-bright);
  background: transparent;
  color: var(--text-dim);
  cursor: pointer;
  transition:
    color 0.2s,
    border-color 0.2s;
}

.boot-mute:hover {
  color: var(--green);
  border-color: var(--green);
}

.boot-skip-hint {
  margin-left: auto;
  color: var(--text-dim);
  font-family: var(--font-pixel);
  font-size: 9px;
  letter-spacing: 1px;
  animation: led-pulse 1.6s steps(2) infinite;
}

@media (prefers-reduced-motion: reduce) {
  .boot-overlay.is-transition,
  .boot-overlay.is-degraded.is-transition {
    animation: none;
  }

  .boot-cursor,
  .boot-skip-hint {
    animation: none;
  }
}
</style>
