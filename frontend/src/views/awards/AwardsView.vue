<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

import Lightbox from '@/components/awards/Lightbox.vue'
import FrontLayout from '@/components/layout/FrontLayout.vue'
import { fetchCertificates, isPdfCertificate, pdfPreviewUrl, type Certificate } from '@/api/certificate'

/**
 * 证书照片墙（Spec 08）：CSS columns 瀑布流 + 缩略图懒加载（进视口前 500px 才加载）
 * + 逐个翻转入场（滚动触发、交错延迟，本页唯一重动效锚点）+ hover 轻视差 + 灯箱看原图。
 * 数据来自 certificate 表（公开接口），后台增删改前台刷新即见。
 */
const certificates = ref<Certificate[]>([])
const loading = ref(true)
const failed = ref(false)

/** 已进入预载区（视口前 500px）的证书 id → 缩略图开始加载 */
const loaded = ref(new Set<number>())
/** 已真正滚入视口的证书 id → 播放翻转入场动画 */
const entered = ref(new Set<number>())

/** 灯箱当前下标（null = 关闭） */
const lightboxIndex = ref<number | null>(null)

const gridEl = ref<HTMLElement | null>(null)

let lazyObserver: IntersectionObserver | null = null
let enterObserver: IntersectionObserver | null = null
let tiltRaf = 0

const TILT_MAX_DEG = 4

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** 触屏设备不绑 hover 视差（触摸滚动不该带动效） */
function supportsHover() {
  return window.matchMedia('(hover: hover)').matches
}

function addTo(set: typeof loaded, id: number) {
  if (set.value.has(id)) return
  const next = new Set(set.value)
  next.add(id)
  set.value = next
}

function targetId(entry: IntersectionObserverEntry): number | null {
  const id = Number((entry.target as HTMLElement).dataset.id)
  return Number.isFinite(id) ? id : null
}

function onLazyIntersect(entries: IntersectionObserverEntry[]) {
  for (const entry of entries) {
    if (!entry.isIntersecting) continue
    lazyObserver?.unobserve(entry.target)
    const id = targetId(entry)
    if (id !== null) addTo(loaded, id)
  }
}

function onEnterIntersect(entries: IntersectionObserverEntry[]) {
  for (const entry of entries) {
    if (!entry.isIntersecting) continue
    enterObserver?.unobserve(entry.target)
    const id = targetId(entry)
    if (id !== null) addTo(entered, id)
  }
}

/** 数据渲染后观察卡片：懒加载提前 500px，翻转入场等真正进视口 */
function observeCards() {
  const root = gridEl.value
  if (!root) return
  const cards = [...root.querySelectorAll<HTMLElement>('.award-card')]
  if (typeof IntersectionObserver === 'undefined') {
    // 无观察器环境（旧浏览器）直接全部可见，防内容永远空白
    for (const card of cards) {
      const id = Number(card.dataset.id)
      if (Number.isFinite(id)) {
        addTo(loaded, id)
        addTo(entered, id)
      }
    }
    return
  }
  lazyObserver = new IntersectionObserver(onLazyIntersect, { rootMargin: '500px' })
  enterObserver = new IntersectionObserver(onEnterIntersect, { rootMargin: '0px 0px -8% 0px' })
  for (const card of cards) {
    lazyObserver.observe(card)
    enterObserver.observe(card)
  }
}

async function load() {
  loading.value = true
  failed.value = false
  try {
    certificates.value = await fetchCertificates()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

/** hover 轻视差：指针位置 → 卡面轻微 3D 倾斜（transform only） */
function onPointerMove(event: PointerEvent) {
  const card = event.currentTarget as HTMLElement | null
  if (!card || prefersReducedMotion() || !supportsHover()) return
  const rect = card.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width - 0.5
  const y = (event.clientY - rect.top) / rect.height - 0.5
  cancelAnimationFrame(tiltRaf)
  tiltRaf = requestAnimationFrame(() => {
    card.style.setProperty('--tilt-y', `${(x * TILT_MAX_DEG).toFixed(2)}deg`)
    card.style.setProperty('--tilt-x', `${(-y * TILT_MAX_DEG).toFixed(2)}deg`)
  })
}

function resetTilt(event: PointerEvent) {
  cancelAnimationFrame(tiltRaf)
  const card = event.currentTarget as HTMLElement | null
  card?.style.setProperty('--tilt-x', '0deg')
  card?.style.setProperty('--tilt-y', '0deg')
}

onMounted(async () => {
  await load()
  await nextTick()
  observeCards()
})

onBeforeUnmount(() => {
  lazyObserver?.disconnect()
  enterObserver?.disconnect()
  cancelAnimationFrame(tiltRaf)
})
</script>

<template>
  <FrontLayout>
    <section class="awards-page" aria-label="证书与奖状照片墙">
      <header class="page-head">
        <p class="prompt">$ ls ~/awards --gallery --lazy</p>
        <h1 class="title">AWARDS</h1>
        <p class="sub">
          <span v-if="loading">scanning trophy wall ...</span>
          <template v-else-if="failed">[error] 暂时无法读取证书数据</template>
          <template v-else-if="certificates.length === 0">
            0 certificates · 等待上传
          </template>
          <template v-else>
            {{ certificates.length }} certificates · hover to preview / click to zoom
          </template>
        </p>
      </header>

      <div v-if="!loading && certificates.length > 0" ref="gridEl" class="awards-grid">
        <button
          v-for="(cert, index) in certificates"
          :key="cert.id"
          type="button"
          class="award-card"
          :class="{ 'in-view': entered.has(cert.id) }"
          :data-id="cert.id"
          :style="{ '--card-delay': `${Math.min(index, 8) * 70}ms` }"
          :aria-label="`查看证书大图：${cert.name}`"
          @click="lightboxIndex = index"
          @pointermove="onPointerMove"
          @pointerleave="resetTilt"
          @pointercancel="resetTilt"
        >
          <span class="award-media">
            <iframe
              v-if="loaded.has(cert.id) && isPdfCertificate(cert)"
              class="award-pdf-preview"
              :src="pdfPreviewUrl(cert.thumbUrl)"
              :title="`PDF 预览：${cert.name}`"
              loading="lazy"
              tabindex="-1"
            />
            <img
              v-else-if="loaded.has(cert.id)"
              class="award-thumb"
              :src="cert.thumbUrl"
              :alt="cert.name"
              decoding="async"
            />
            <span v-else class="award-skeleton" aria-hidden="true"></span>
            <span class="award-overlay">
              <span class="overlay-name">{{ cert.name }}</span>
              <span class="overlay-date">{{ cert.obtainedAt }}</span>
            </span>
          </span>
        </button>
      </div>

      <div v-else-if="!loading && !failed" class="empty-state">
        <div class="hud-frame" aria-hidden="true"></div>
        <p class="empty-line">$ ls ~/certificates</p>
        <p class="empty-line dim">证书照片墙尚未上传，稍后再来 ——</p>
      </div>

      <Lightbox v-model:active="lightboxIndex" :items="certificates" />
    </section>
  </FrontLayout>
</template>

<style scoped>
.awards-page {
  position: relative;
  width: min(1200px, 92vw);
  margin: 0 auto;
  padding: 48px 0 64px;
}

.page-head {
  margin-bottom: 24px;
}

.prompt {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 20px;
}

.title {
  margin: 0;
  font-family: var(--font-term);
  font-size: clamp(36px, 6vw, 56px);
  color: var(--green);
  text-shadow:
    0 0 10px var(--green-glow),
    0 0 36px var(--green-glow);
}

.sub {
  margin: 4px 0 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 19px;
}

/* ---- 瀑布流：桌面 4 列 → 平板 3 列 → 移动 2 列（无横向滚动） ---- */
.awards-grid {
  columns: 4;
  column-gap: 16px;
}

@media (max-width: 1080px) {
  .awards-grid {
    columns: 3;
  }
}

@media (max-width: 720px) {
  .awards-grid {
    columns: 2;
    column-gap: 12px;
  }
}

/* ---- 卡片：逐个翻转入场（滚动触发、交错延迟） ---- */
.award-card {
  display: block;
  width: 100%;
  margin: 0 0 16px;
  padding: 0;
  border: none;
  background: transparent;
  font: inherit;
  color: inherit;
  cursor: pointer;
  break-inside: avoid;
  opacity: 0;
  transform-origin: 50% 100%;
  -webkit-appearance: none;
  appearance: none;
}

.award-card.in-view {
  animation: award-flip-in 0.55s cubic-bezier(0.23, 1, 0.32, 1) both;
  animation-delay: var(--card-delay);
}

@keyframes award-flip-in {
  from {
    opacity: 0;
    transform: perspective(900px) rotateX(30deg) translateY(24px);
  }
  to {
    opacity: 1;
    transform: perspective(900px) rotateX(0deg) translateY(0);
  }
}

/* ---- 卡面：hover 轻视差（transform only） ---- */
.award-media {
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  position: relative;
  display: block;
  overflow: hidden;
  border: 2px solid var(--border);
  background: var(--bg-panel);
  transform: perspective(900px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y));
  transition:
    transform 0.15s ease-out,
    border-color 0.2s,
    box-shadow 0.2s;
}

.award-card:hover .award-media,
.award-card:focus-visible .award-media {
  border-color: var(--border-bright);
  box-shadow: 0 0 18px var(--green-soft);
}

.award-thumb {
  display: block;
  width: 100%;
  height: auto;
}

.award-pdf-preview {
  display: block;
  width: 100%;
  height: 380px;
  border: 0;
  background: var(--bg-raised);
  pointer-events: none;
}

/* 加载占位：像素扫描线 + LED 呼吸（不动布局） */
.award-skeleton {
  display: block;
  width: 100%;
  min-height: 140px;
  background-color: var(--bg-raised);
  background-image: repeating-linear-gradient(180deg, var(--border) 0 1px, transparent 1px 6px);
  animation: led-pulse 2.4s ease-in-out infinite;
}

/* ---- hover 浮层：证书名称 + 获取时间 ---- */
.award-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 4px;
  padding: 12px;
  background: linear-gradient(
    180deg,
    transparent 45%,
    color-mix(in srgb, var(--bg) 78%, transparent)
  );
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.award-card:hover .award-overlay,
.award-card:focus-visible .award-overlay {
  opacity: 1;
}

.overlay-name {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.4;
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
  word-break: break-all;
}

.overlay-date {
  font-family: var(--font-term);
  font-size: 16px;
  color: var(--text);
}

.empty-state {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 56px 24px;
  border: 2px solid var(--border);
  background: var(--bg-panel);
}

.empty-line {
  margin: 0;
  font-family: var(--font-term);
  font-size: 20px;
  color: var(--text);
}

.empty-line.dim {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-dim);
}

@media (prefers-reduced-motion: reduce) {
  .award-card {
    opacity: 1;
  }

  .award-card.in-view {
    animation: none;
  }

  .award-media {
    transform: none;
    transition: none;
  }

  .award-skeleton {
    animation: none;
  }

  .award-overlay {
    transition: none;
  }
}
</style>
