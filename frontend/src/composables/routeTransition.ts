import { reactive } from 'vue'
import type { Router } from 'vue-router'

/**
 * 路由过场编排：CRT 断电 -> 终端引导 -> CRT 通电
 *
 * 通过 installRouteTransition(router) 显式安装（仅 main.ts），
 * 守卫内部取消原始导航、播放动画后再以放行标记重新发起，
 * 因此单测直接 import router 时不受任何影响。
 */

export type TransitionPhase = 'idle' | 'collapse' | 'boot' | 'reveal'

export const routeTransition = reactive({
  phase: 'idle' as TransitionPhase,
  target: '',
})

const COLLAPSE_MS = 460
const BOOT_MS = 640
const REVEAL_MS = 560

/** 过场期间内部导航（含认证重定向）直接放行 */
let allowPass = false
/** 过场序列是否进行中（期间忽略外部点击） */
let running = false

function wait(ms: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function installRouteTransition(router: Router) {
  router.beforeEach((to) => {
    if (allowPass) return true
    if (running) return false
    if (to.fullPath === router.currentRoute.value.fullPath) return true
    if (prefersReducedMotion()) return true

    void runSequence(router, to.fullPath)
    return false
  })
}

async function runSequence(router: Router, fullPath: string) {
  running = true
  routeTransition.target = fullPath

  // 1. CRT 断电：旧画面压扁成亮线熄灭
  routeTransition.phase = 'collapse'
  await wait(COLLAPSE_MS)

  // 2. 终端引导：黑屏打字 + 像素进度条，期间完成真实导航
  routeTransition.phase = 'boot'
  allowPass = true
  try {
    await Promise.all([wait(BOOT_MS), router.push(fullPath)])
  } catch {
    // 导航失败也继续收尾，避免过场卡死
  }

  // 3. CRT 通电：新页面从亮线展开 + 扫描光束 + 故障闪切
  routeTransition.phase = 'reveal'
  await wait(REVEAL_MS)

  routeTransition.phase = 'idle'
  routeTransition.target = ''
  allowPass = false
  running = false
}
