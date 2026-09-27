import type { Router } from 'vue-router'

import { sendBeaconJson } from './beacon'
import { getSessionId } from './session'

/**
 * 埋点 SDK（Spec 05，全站横切件）：Spec 03/04/10/11 统一经由 trackEvent 上报，勿各自实现。
 *
 * - 会话开始：进入站点时一次 POST /api/track/session（后端按 sessionId 幂等）
 * - 会话结束：pagehide / visibilitychange hidden 时 POST /api/track/session/{sid}/end（sendBeacon 优先）
 * - 路由变化：router.afterEach 自动 POST /api/track/event {eventType:'page_view', pagePath}
 * - 统一导出：trackEvent(eventType, detail)，sendBeacon 优先、失败静默
 */

export { SESSION_KEY, getSessionId } from './session'
export { sendBeaconJson } from './beacon'

const SESSION_START_URL = '/api/track/session'
const EVENT_URL = '/api/track/event'

let installed = false
/** 最近一次页面路径（会话结束上报 lastPagePath 用） */
let lastPagePath = '/'
let pagehideHandler: (() => void) | null = null
let visibilityHandler: (() => void) | null = null

/** 统一事件入口：全站埋点都走这里（fire-and-forget，不抛错不阻塞） */
export function trackEvent(eventType: string, detail?: unknown): void {
  sendBeaconJson(EVENT_URL, {
    sessionId: getSessionId(),
    eventType,
    pagePath: window.location.pathname,
    ...(detail === undefined ? {} : { detail }),
  })
}

/** 会话结束上报（后端幂等：已结束则忽略，重复触发无害） */
function endSession(): void {
  sendBeaconJson(`${SESSION_START_URL}/${getSessionId()}/end`, {
    lastPagePath: window.location.pathname || lastPagePath,
  })
}

/**
 * 全站安装（src/main.ts 一行）：会话开始 + 路由 page_view + 离开上报。
 * 幂等：重复调用不重复安装。
 */
export function installTracker(router: Router): void {
  if (installed) return
  installed = true

  // 会话开始（进入站点一次；referrer 缺省不上报）
  sendBeaconJson(SESSION_START_URL, {
    sessionId: getSessionId(),
    ...(document.referrer ? { referrer: document.referrer } : {}),
    entryPage: window.location.pathname,
  })

  // 路由变化自动 page_view（afterEach 对初始导航也触发一次）
  router.afterEach((to) => {
    lastPagePath = to.path
    trackEvent('page_view')
  })

  // 会话结束：关页/切后台（sendBeacon 在卸载阶段仍可入队）
  pagehideHandler = endSession
  visibilityHandler = () => {
    if (document.visibilityState === 'hidden') endSession()
  }
  window.addEventListener('pagehide', pagehideHandler)
  document.addEventListener('visibilitychange', visibilityHandler)
}

/** 仅测试用：卸载监听器并重置安装状态（真实业务代码勿调） */
export function resetTrackerForTest(): void {
  if (pagehideHandler) window.removeEventListener('pagehide', pagehideHandler)
  if (visibilityHandler) document.removeEventListener('visibilitychange', visibilityHandler)
  pagehideHandler = null
  visibilityHandler = null
  installed = false
  lastPagePath = '/'
}
