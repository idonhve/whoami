<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { trackGithubOutbound } from '@/api/track'
import type { WorkCard } from '@/api/works'
import { langColor, relativeTime } from './repoLanguages'

/**
 * 作品卡片（Spec 04）：hover 3D 倾斜（/works 唯一 3D 锚点）+ 滚动渐入 + 新标签外跳 GitHub。
 * 仓库名从 htmlUrl 尾段推导（公开 DTO 契约不含 repo_name）。
 */
const props = defineProps<{ work: WorkCard }>()

const cardEl = ref<HTMLElement | null>(null)

const repoName = computed(() => {
  const segments = props.work.htmlUrl.split('/').filter(Boolean)
  return segments[segments.length - 1] ?? props.work.htmlUrl
})
const langDotStyle = computed(() => ({ '--lang-dot': langColor(props.work.language) }))
const updated = computed(() => relativeTime(props.work.pushedAt))
const desc = computed(() => props.work.cnTitle || '')

const TILT_MAX_DEG = 5
let tiltRaf = 0
let observer: IntersectionObserver | null = null

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** 触屏设备不绑 3D 倾斜（触摸滚动不该带动效） */
function supportsHover() {
  return window.matchMedia('(hover: hover)').matches
}

function onPointerMove(event: PointerEvent) {
  const card = cardEl.value
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

function resetTilt() {
  cancelAnimationFrame(tiltRaf)
  cardEl.value?.style.setProperty('--tilt-x', '0deg')
  cardEl.value?.style.setProperty('--tilt-y', '0deg')
}

function onCardClick() {
  trackGithubOutbound(repoName.value)
}

onMounted(() => {
  const card = cardEl.value
  if (!card) return
  if (typeof IntersectionObserver === 'undefined') {
    card.classList.add('in-view')
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view')
          observer?.unobserve(entry.target)
        }
      }
    },
    { rootMargin: '0px 0px -8% 0px' },
  )
  observer.observe(card)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  cancelAnimationFrame(tiltRaf)
})
</script>

<template>
  <article
    ref="cardEl"
    class="repo-card"
    :style="langDotStyle"
    @pointermove="onPointerMove"
    @pointerleave="resetTilt"
    @pointercancel="resetTilt"
  >
    <a
      :href="work.htmlUrl"
      target="_blank"
      rel="noopener noreferrer"
      class="card-link"
      :aria-label="`打开 GitHub 仓库 ${repoName}`"
      @click="onCardClick"
    >
      <header class="card-head">
        <span class="repo-name">{{ repoName }}</span>
        <span v-if="work.isPinned" class="pin-tag" title="置顶作品">[PIN]</span>
      </header>

      <p class="card-desc" :class="{ empty: !desc }">{{ desc || '(no description)' }}</p>

      <footer class="card-meta">
        <span class="meta-item lang">
          <i class="lang-dot" aria-hidden="true"></i>{{ work.language ?? 'unknown' }}
        </span>
        <span class="meta-item">
          <svg class="meta-icon" viewBox="0 0 16 16" aria-hidden="true">
            <path
              d="M8 1.5l1.9 3.9 4.3.6-3.1 3 .7 4.2L8 11.2l-3.8 2 .7-4.2-3.1-3 4.3-.6z"
              fill="currentColor"
            />
          </svg>{{ work.stargazersCount }}
        </span>
        <span class="meta-item">
          <svg class="meta-icon" viewBox="0 0 16 16" aria-hidden="true">
            <g fill="currentColor">
              <rect x="3" y="1" width="2.6" height="2.6" />
              <rect x="10.4" y="1" width="2.6" height="2.6" />
              <rect x="6.7" y="12.4" width="2.6" height="2.6" />
              <path
                d="M4.3 4.5v1.7c0 1.1.9 2 2 2h3.4c1.1 0 2-.9 2-2V4.5h-1.4v1.7c0 .3-.3.6-.6.6H6.3c-.3 0-.6-.3-.6-.6V4.5z"
              />
              <rect x="7.3" y="8" width="1.4" height="4.5" />
            </g>
          </svg>{{ work.forksCount }}
        </span>
        <span class="meta-item updated" :title="work.pushedAt ?? ''">{{ updated }}</span>
      </footer>
    </a>
  </article>
</template>

<style scoped>
.repo-card {
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  --card-delay: 0ms;
  opacity: 0;
  transform: perspective(800px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y));
  transition: transform 0.15s ease-out;
}

.repo-card.in-view {
  animation: rise-in 0.5s cubic-bezier(0.23, 1, 0.32, 1) both;
  animation-delay: var(--card-delay);
}

.card-link {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 44px;
  padding: 16px 18px 14px;
  border: 2px solid var(--border);
  background: var(--bg-panel);
  color: var(--text);
  text-decoration: none;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background 0.2s;
}

.repo-card:hover .card-link,
.repo-card:focus-within .card-link {
  border-color: var(--border-bright);
  background: var(--bg-raised);
  box-shadow: 0 0 18px var(--green-soft);
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.repo-name {
  font-family: var(--font-pixel);
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
  word-break: break-all;
}

.pin-tag {
  flex-shrink: 0;
  font-family: var(--font-pixel);
  font-size: 8px;
  letter-spacing: 1px;
  color: var(--amber);
  border: 1px solid var(--amber);
  padding: 3px 5px;
  text-shadow: 0 0 6px var(--green-glow);
}

.card-desc {
  flex: 1;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc.empty {
  color: var(--text-dim);
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--text-dim);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.meta-item.lang {
  color: var(--text);
}

.lang-dot {
  width: 8px;
  height: 8px;
  background: var(--lang-dot, var(--lang-fallback));
  box-shadow: 0 0 6px var(--lang-dot, var(--lang-fallback));
}

.meta-icon {
  width: 12px;
  height: 12px;
}

.meta-item.updated {
  margin-left: auto;
}

@media (prefers-reduced-motion: reduce) {
  .repo-card {
    opacity: 1;
    transform: none;
    transition: none;
  }

  .repo-card.in-view {
    animation: none;
  }
}
</style>
