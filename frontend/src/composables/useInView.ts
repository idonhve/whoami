import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/**
 * 滚动进入视口触发：一旦可见即置 true 并停止观察（动画只播一次）。
 * 环境无 IntersectionObserver（如 happy-dom 单测）时直接放行为可见，保证测试可测。
 */
export function useInView(target: Ref<HTMLElement | null>, options?: IntersectionObserverInit): Ref<boolean> {
  const inView = ref(false)
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (typeof IntersectionObserver === 'undefined') {
      inView.value = true
      return
    }
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            inView.value = true
            observer?.disconnect()
          }
        }
      },
      options ?? { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
    )
    if (target.value) observer.observe(target.value)
  })

  onBeforeUnmount(() => observer?.disconnect())

  return inView
}