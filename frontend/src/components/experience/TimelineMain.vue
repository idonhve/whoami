<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import type { Experience } from '@/api/experience'
import ExpandableCard from './ExpandableCard.vue'

/**
 * 经历时间轴主线（Spec 09）：本页唯一 3D/重动效锚点。
 * 桌面：左侧发光主线随滚动逐段点亮，当前经历卡片高亮（GSAP ScrollTrigger 驱动 activeIndex）。
 * 移动端 / reduced-motion：动画简化——单列卡片 + 滚动渐入，主线变细，用轻量 IntersectionObserver 驱动高亮（无 ScrollTrigger 重计算泄漏）。
 */
defineProps<{ experiences: Experience[] }>()

const root = ref<HTMLElement | null>(null)
const activeIndex = ref(-1)

let triggers: ScrollTrigger[] = []
let observer: IntersectionObserver | null = null
let disposed = false

function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  )
}

function isDesktop(): boolean {
  return typeof window !== 'undefined' && window.innerWidth >= 721
}

function setupScrollTrigger() {
  gsap.registerPlugin(ScrollTrigger)
  const cards = Array.from(root.value?.querySelectorAll<HTMLElement>('[data-exp-index]') ?? [])
  cards.forEach((card, index) => {
    triggers.push(
      ScrollTrigger.create({
        trigger: card,
        start: 'top 62%',
        end: 'bottom 38%',
        onToggle: (self) => {
          if (disposed) return
          if (self.isActive) activeIndex.value = index
        },
      }),
    )
  })
  ScrollTrigger.refresh()
}

function setupLightweightIndex() {
  if (typeof IntersectionObserver === 'undefined') {
    activeIndex.value = 0
    return
  }
  const cards = Array.from(root.value?.querySelectorAll<HTMLElement>('[data-exp-index]') ?? [])
  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting || disposed) continue
        const index = Number(entry.target.getAttribute('data-exp-index'))
        if (Number.isInteger(index) && index >= 0) activeIndex.value = index
      }
    },
    { rootMargin: '-30% 0px -40% 0px', threshold: 0 },
  )
  cards.forEach((card) => observer?.observe(card))
}

function isLit(index: number): boolean {
  return activeIndex.value >= 0 && index <= activeIndex.value
}

onMounted(() => {
  if (prefersReducedMotion() || !isDesktop()) {
    setupLightweightIndex()
    return
  }
  setupScrollTrigger()
})

onBeforeUnmount(() => {
  disposed = true
  observer?.disconnect()
  observer = null
  for (const trigger of triggers) trigger.kill()
  triggers = []
  ScrollTrigger.refresh()
})
</script>

<template>
  <div
    ref="root"
    class="timeline"
    :data-active-index="activeIndex"
    data-testid="experience-timeline"
  >
    <span class="rail" aria-hidden="true"></span>
    <ul class="segments" aria-hidden="true">
      <li
        v-for="(exp, i) in experiences"
        :key="exp.id"
        class="segment"
        :class="{ '--lit': isLit(i) }"
      ></li>
    </ul>
    <ol class="cards">
      <li v-for="(exp, i) in experiences" :key="exp.id" class="card-slot" :data-exp-index="i">
        <span class="node" aria-hidden="true" :class="{ '--active': activeIndex === i }"></span>
        <ExpandableCard
          class="expandable"
          :class="{ '--active': activeIndex === i }"
          :experience="exp"
        />
      </li>
    </ol>
  </div>
</template>

<style scoped>
.timeline {
  position: relative;
}

/* 细轨（总行程） */
.rail {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 15px;
  width: 2px;
  background: var(--border);
}

/* 逐段点亮的主线（覆盖在 rail 上随滚动点亮） */
.segments {
  list-style: none;
  margin: 0;
  padding: 0;
  position: absolute;
  top: 0;
  left: 15px;
  width: 3px;
  transform: translateX(-0.5px);
  display: flex;
  flex-direction: column;
}

.segment {
  flex: 1;
  background: transparent;
  box-shadow: none;
  transition:
    background 0.4s,
    box-shadow 0.4s;
}

.segment.--lit {
  background: var(--green);
  box-shadow: 0 0 12px var(--green-glow);
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-left: 44px;
}

.card-slot {
  position: relative;
}

.node {
  position: absolute;
  left: -38px;
  top: 18px;
  width: 12px;
  height: 12px;
  border: 2px solid var(--border-bright);
  border-radius: 50%;
  background: var(--bg);
  transition:
    background 0.25s,
    border-color 0.25s,
    box-shadow 0.25s;
}

.node.--active {
  border-color: var(--green);
  background: var(--green);
  box-shadow: 0 0 14px var(--green-glow);
}

.expandable.--active {
  border-color: var(--green);
  box-shadow: 0 0 22px var(--green-soft);
}

@media (max-width: 720px) {
  /* 移动端：主线细化为左侧细线，简化动效 */
  .timeline {
    padding-left: 6px;
  }
  .rail {
    left: 4px;
    width: 1px;
  }
  .segments {
    left: 4px;
    width: 2px;
    transform: none;
  }
  .cards {
    padding-left: 22px;
  }
  .node {
    left: -18px;
    top: 16px;
    width: 8px;
    height: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .segment,
  .node,
  .expandable {
    transition: none;
  }
}
</style>
