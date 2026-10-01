<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

import { cssVar } from '@/utils/cssVar'
import { prefersReducedMotion } from '@/utils/motion'

/**
 * F11 秘籍粒子雨（Spec 11）：2D canvas 轻量实现，严禁 Three.js（不占页面 3D 锚点配额）。
 * 瞬时全局层：pointer-events:none + aria-hidden，不抢焦点、不改变 DOM 语义；
 * 数秒后自然消散 —— 停止 rAF 并向宿主发 finished，由宿主卸载并移除 canvas 节点。
 */
const emit = defineEmits<{ finished: [] }>()

/** 粒子（雨滴）数量上限（Spec 11：轻量，约 120 封顶） */
const MAX_DROPS = 120
/** 粒子雨总时长：含尾部整体淡出，之后停止 rAF */
const RAIN_MS = 4200
/** 结束前的整体淡出时长 */
const FADE_MS = 700

interface Drop {
  /** 雨滴起点横坐标 */
  x: number
  /** 雨滴头部纵坐标 */
  y: number
  /** 下落速度（px / 帧） */
  speed: number
  /** 雨滴长度 */
  len: number
  /** 雨滴宽度（1~2px，像素感） */
  width: number
  /** 颜色（设计 token 运行时读取） */
  color: string
}

const reduced = prefersReducedMotion()
const canvasRef = ref<HTMLCanvasElement | null>(null)

let raf = 0
let finished = false

function spawnDrop(w: number, h: number, palette: string[], initial: boolean): Drop {
  return {
    x: Math.random() * w,
    y: initial ? -Math.random() * h : -Math.random() * 60,
    speed: 3 + Math.random() * 5,
    len: 8 + Math.random() * 18,
    width: Math.random() < 0.5 ? 1 : 2,
    color: palette[Math.floor(Math.random() * palette.length)] ?? palette[0],
  }
}

function stopDraw(): void {
  if (raf) {
    cancelAnimationFrame(raf)
    raf = 0
  }
}

/** 通知宿主清理：异步发出，避免在自身挂载过程中同步自卸载 */
function finish(): void {
  if (finished) return
  finished = true
  stopDraw()
  window.setTimeout(() => emit('finished'), 0)
}

onMounted(() => {
  if (reduced) {
    // reduced-motion 降级（Spec 11 硬性约束）：不渲染粒子雨，宿主仅保留 toast
    finish()
    return
  }
  const canvas = canvasRef.value
  const ctx = canvas?.getContext('2d') ?? null
  if (!canvas || !ctx) {
    // 无 2D 上下文（单测环境 / 极端环境）：直接走完成路径，不留悬挂层
    finish()
    return
  }

  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const w = window.innerWidth
  const h = window.innerHeight
  canvas.width = w * dpr
  canvas.height = h * dpr
  ctx.scale(dpr, dpr)

  // 颜色唯一来源仍是设计 token（Canvas 无法直接引用 CSS 变量，fallback 与 :root 一致）
  const palette = [
    cssVar('--green', '#00ff9c'),
    cssVar('--cyan', '#2bd9ff'),
    cssVar('--magenta', '#ff2e88'),
    cssVar('--text', '#c8d6e5'),
  ]

  const drops: Drop[] = Array.from({ length: MAX_DROPS }, () => spawnDrop(w, h, palette, true))
  const start = performance.now()

  const frame = () => {
    const t = performance.now() - start
    // 尾部整体淡出，让粒子雨"自然消散"
    ctx.globalAlpha = t < RAIN_MS - FADE_MS ? 1 : Math.max(0, (RAIN_MS - t) / FADE_MS)
    ctx.clearRect(0, 0, w, h)
    for (const drop of drops) {
      drop.y += drop.speed
      if (drop.y - drop.len > h) Object.assign(drop, spawnDrop(w, h, palette, false))
      ctx.fillStyle = drop.color
      ctx.fillRect(Math.round(drop.x), Math.round(drop.y - drop.len), drop.width, drop.len)
    }
    if (t >= RAIN_MS) {
      finish()
      return
    }
    raf = requestAnimationFrame(frame)
  }
  raf = requestAnimationFrame(frame)
})

onBeforeUnmount(stopDraw)
</script>

<template>
  <canvas v-if="!reduced" ref="canvasRef" class="konami-rain" aria-hidden="true"></canvas>
</template>

<style scoped>
.konami-rain {
  position: fixed;
  inset: 0;
  /* 位于全局 CRT 质感层(9999)之下的瞬时全局层 */
  z-index: 9990;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
