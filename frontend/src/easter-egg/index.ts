/**
 * F11 控制台彩蛋 + 键盘秘籍（Spec 11）入口。
 * App.vue 挂载后调用一次 initEasterEgg()：
 * - 一次性输出 console 彩蛋（签名 + 招聘联系方式 + "对的人"文案，见 consoleArt）
 * - 秘籍监听惰性绑定：未进入候选状态时，非首键（↑）O(1) 返回，不建匹配器、不比对
 * - 触发时：easter_egg 埋点（fire-and-forget）+ 终端风 toast + 粒子雨
 *   （prefers-reduced-motion 时粒子雨降级为仅 toast）
 * 彩蛋是瞬时全局层：不抢焦点、不改变 DOM 语义、不占任何页面 3D 锚点配额。
 */
import { createApp, h } from 'vue'

import { trackEasterEgg } from '@/api/track'
import { prefersReducedMotion } from '@/utils/motion'
import ParticleRain from './ParticleRain.vue'
import { printConsoleEgg } from './consoleArt'
import { KONAMI_SEQUENCE, createKonamiMatcher, type KonamiMatcher } from './konami'
import { showKonamiToast } from './toast'

/** 已绑定的 keydown 监听（重复 init 时先解绑，保证幂等） */
let boundListener: ((event: KeyboardEvent) => void) | null = null
/** 候选序列匹配器：null = 未进入候选状态（零开销路径） */
let matcher: KonamiMatcher | null = null

function onTrigger(): void {
  // 埋点 fire-and-forget：后端 /api/track/event 未就绪时静默失败，不影响彩蛋
  trackEasterEgg('konami')
  showKonamiToast()
  if (prefersReducedMotion()) return // reduced-motion：粒子雨降级为仅 toast
  runParticleRain()
}

/** 粒子雨挂到一次性宿主节点上，finished 后自卸载并移除节点 */
function runParticleRain(): void {
  const host = document.createElement('div')
  document.body.appendChild(host)
  const app = createApp({
    render: () =>
      h(ParticleRain, {
        onFinished: () => {
          app.unmount()
          host.remove()
        },
      }),
  })
  app.mount(host)
}

function onKeydown(event: KeyboardEvent): void {
  // 组合键属于应用快捷键语义（如 Ctrl+K），不参与秘籍
  if (event.ctrlKey || event.metaKey || event.altKey) return
  if (!matcher) {
    // O(1) 退出：非候选首键（↑）不进入匹配状态
    if (event.key !== KONAMI_SEQUENCE[0]) return
    matcher = createKonamiMatcher()
  }
  const state = matcher.press(event.key)
  if (state === 'completed') {
    matcher = null
    onTrigger()
  } else if (state === 'idle') {
    // 错误序列 / 窗口超时：中途重置并退出候选状态，下个 ↑ 重新开始
    matcher = null
  }
}

/** 应用挂载后调用（App.vue onMounted）：输出 console 彩蛋并绑定秘籍监听 */
export function initEasterEgg(): void {
  printConsoleEgg()
  matcher = null
  if (boundListener) window.removeEventListener('keydown', boundListener)
  boundListener = onKeydown
  window.addEventListener('keydown', boundListener)
}
