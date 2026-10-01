<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { gsap } from 'gsap'

import { useInView } from '@/composables/useInView'
import { parseCounter } from './experienceFormat'

/**
 * 战果数字翻牌（Spec 09）：核心数字战果（如 "300%"、"50w+"）进入视口后 GSAP 递增，
 * 文本后缀常显仅数字部分动画。纯文本值（无前导数字）直接显示，不走动画。
 * 测试友好：动画目标与后缀可从渲染文本观察（data-testid="achievement-counter"）。
 */
const props = defineProps<{ value: string }>()

const { num, suffix } = parseCounter(props.value)

const el = ref<HTMLElement | null>(null)
const visible = useInView(el)

const decimals =
  num !== null && Number.isInteger(num)
    ? 0
    : Math.max(0, String(num ?? '').split('.')[1]?.length ?? 0)
const cur = ref(0)

function reduceMotion(): boolean {
  return (
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  )
}

watch(
  visible,
  (v) => {
    if (!v || num === null) return
    // reduced-motion：数字直出最终值，不做递增动画（MASTER 铁律）
    if (reduceMotion()) {
      cur.value = num
      return
    }
    const target = { v: 0 }
    gsap.to(target, {
      v: num,
      duration: 1,
      ease: 'power2.out',
      onUpdate: () => {
        cur.value = Number(target.v.toFixed(decimals))
      },
      onComplete: () => {
        cur.value = num
      },
    })
  },
  { immediate: true },
)

const display = computed(() => {
  if (num === null) return props.value.trim()
  return `${cur.value}${suffix}`
})
</script>

<template>
  <span ref="el" class="counter" data-testid="achievement-counter">{{ display }}</span>
</template>

<style scoped>
.counter {
  display: inline-block;
  font-family: var(--font-term);
  font-size: 40px;
  line-height: 1;
  color: var(--green);
  text-shadow:
    0 0 10px var(--green-glow),
    0 0 28px var(--green-glow);
  white-space: nowrap;
}
</style>
