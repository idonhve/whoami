<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { isPdfCertificate, pdfPreviewUrl, type Certificate } from '@/api/certificate'

/**
 * 证书灯箱（Spec 08）：打开时才加载压缩原图；
 * 左右切换（按钮 + 键盘 ←/→，列表内环游）、ESC / 点击遮罩关闭。
 * 切换为常规 opacity/transform 过渡，不是重动效锚点（锚点在 /awards 网格卡片）。
 */
const props = defineProps<{
  /** 全量证书列表（切换在下标环游，不重新排序） */
  items: Certificate[]
}>()

/** 当前激活下标；null = 关闭（v-model:active） */
const active = defineModel<number | null>('active', { default: null })

const dialogEl = ref<HTMLElement | null>(null)

const current = computed(() => {
  const index = active.value
  if (index === null || index < 0 || index >= props.items.length) return null
  return props.items[index] ?? null
})

function close() {
  active.value = null
}

function step(delta: number) {
  const total = props.items.length
  if (total === 0 || active.value === null) return
  active.value = (active.value + delta + total) % total
}

function onKeydown(event: KeyboardEvent) {
  if (active.value === null) return
  if (event.key === 'Escape') {
    event.preventDefault()
    close()
  } else if (event.key === 'ArrowLeft') {
    event.preventDefault()
    step(-1)
  } else if (event.key === 'ArrowRight') {
    event.preventDefault()
    step(1)
  }
}

// 打开时锁定页面滚动并把焦点移入对话框（键盘用户不落到背景）；关闭/卸载时还原
watch(
  active,
  (index) => {
    document.body.classList.toggle('lb-scroll-lock', index !== null)
    if (index !== null) {
      void nextTick(() => dialogEl.value?.focus())
    }
  },
  { immediate: true },
)

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  document.body.classList.remove('lb-scroll-lock')
})
</script>

<template>
  <Transition name="lb">
    <div
      v-if="current"
      ref="dialogEl"
      class="lb-backdrop"
      role="dialog"
      aria-modal="true"
      :aria-label="`查看证书 ${current.name}`"
      tabindex="-1"
      @click.self="close"
    >
      <figure class="lb-stage">
        <Transition name="lb-img" mode="out-in">
          <iframe
            v-if="isPdfCertificate(current)"
            :key="current.id"
            class="lb-pdf"
            :src="pdfPreviewUrl(current.imageUrl)"
            :title="`PDF 证书：${current.name}`"
          />
          <img
            v-else
            :key="current.id"
            class="lb-image"
            :src="current.imageUrl"
            :alt="current.name"
            decoding="async"
          />
        </Transition>
        <figcaption class="lb-caption">
          <span class="lb-name">{{ current.name }}</span>
          <span class="lb-meta">
            <span class="lb-count">{{ (active ?? 0) + 1 }} / {{ items.length }}</span>
            <span class="lb-date">{{ current.obtainedAt }}</span>
          </span>
        </figcaption>
      </figure>

      <button type="button" class="lb-btn lb-prev" aria-label="上一张" @click="step(-1)">
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path d="M10.5 2.5 5 8l5.5 5.5" fill="none" stroke="currentColor" stroke-width="2" />
        </svg>
      </button>
      <button type="button" class="lb-btn lb-next" aria-label="下一张" @click="step(1)">
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path d="M5.5 2.5 11 8l-5.5 5.5" fill="none" stroke="currentColor" stroke-width="2" />
        </svg>
      </button>
      <button type="button" class="lb-btn lb-close" aria-label="关闭灯箱" @click="close">
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path d="M3 3l10 10M13 3L3 13" fill="none" stroke="currentColor" stroke-width="2" />
        </svg>
      </button>
    </div>
  </Transition>
</template>

<style>
/* 灯箱打开时锁背景滚动（scoped 无法选中 body，类名带 lb- 前缀防碰撞） */
body.lb-scroll-lock {
  overflow: hidden;
}
</style>

<style scoped>
.lb-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9000; /* CRT 质感层(9999) 之下、其余内容之上 */
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--bg) 90%, transparent);
  backdrop-filter: blur(2px);
  outline: none;
}

.lb-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin: 0;
  max-width: 94vw;
}

.lb-image {
  display: block;
  max-width: 92vw;
  max-height: 78vh;
  border: 2px solid var(--border-bright);
  background: var(--bg-panel);
  box-shadow: 0 0 32px var(--green-soft);
  object-fit: contain;
}

.lb-pdf {
  display: block;
  width: min(92vw, 960px);
  height: 78vh;
  border: 2px solid var(--border-bright);
  background: var(--bg-panel);
  box-shadow: 0 0 32px var(--green-soft);
}

.lb-caption {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  max-width: 92vw;
  text-align: center;
}

.lb-name {
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
  word-break: break-all;
}

.lb-meta {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-dim);
}

.lb-count {
  font-family: var(--font-pixel);
  font-size: 9px;
  letter-spacing: 1px;
  color: var(--cyan);
}

.lb-btn {
  position: absolute;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 2px solid var(--border-bright);
  background: color-mix(in srgb, var(--bg-raised) 88%, transparent);
  color: var(--text);
  cursor: pointer;
  transition:
    border-color 0.2s,
    color 0.2s,
    box-shadow 0.2s,
    transform 0.15s;
}

.lb-btn svg {
  width: 18px;
  height: 18px;
}

.lb-btn:hover,
.lb-btn:focus-visible {
  border-color: var(--green);
  color: var(--green);
  box-shadow: 0 0 14px var(--green-soft);
}

.lb-prev {
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
}

.lb-next {
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
}

.lb-close {
  right: 16px;
  top: 16px;
}

.lb-prev:hover,
.lb-prev:focus-visible {
  transform: translateY(-50%) translateX(-2px);
}

.lb-next:hover,
.lb-next:focus-visible {
  transform: translateY(-50%) translateX(2px);
}

/* 打开/关闭：常规 opacity + 轻缩放过渡 */
.lb-enter-active,
.lb-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.lb-enter-from,
.lb-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

/* 切图：交叉淡入淡出 */
.lb-img-enter-active,
.lb-img-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.lb-img-enter-from {
  opacity: 0;
  transform: scale(0.985);
}

.lb-img-leave-to {
  opacity: 0;
}

@media (max-width: 560px) {
  .lb-image {
    max-width: 94vw;
    max-height: 72vh;
  }

  .lb-pdf {
    width: 94vw;
    height: 72vh;
  }

  .lb-prev {
    left: 8px;
  }

  .lb-next {
    right: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .lb-btn {
    transition: none;
  }

  .lb-enter-active,
  .lb-leave-active,
  .lb-img-enter-active,
  .lb-img-leave-active {
    transition: none;
  }
}
</style>
