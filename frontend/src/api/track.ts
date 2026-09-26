/**
 * 埋点上报（Spec 05 契约：POST /api/track/event {sessionId, eventType, pagePath, detail}）。
 * F4 只用 fire-and-forget 方式上报 github_outbound：接口未就绪时静默失败，不影响外跳；
 * Spec 05 交付 SDK 后由其统一接管，本函数保持调用点不变。
 */
const SESSION_KEY = 'whoami_track_session'

function sessionId(): string {
  let id = localStorage.getItem(SESSION_KEY)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(SESSION_KEY, id)
  }
  return id
}

/** 卡片外跳 GitHub 计入埋点（不 await、不抛错） */
export function trackGithubOutbound(repoName: string): void {
  try {
    const payload = JSON.stringify({
      sessionId: sessionId(),
      eventType: 'github_outbound',
      pagePath: window.location.pathname,
      detail: repoName,
    })
    if (navigator.sendBeacon) {
      navigator.sendBeacon(
        '/api/track/event',
        new Blob([payload], { type: 'application/json' }),
      )
    } else {
      void fetch('/api/track/event', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: payload,
        keepalive: true,
      }).catch(() => {})
    }
  } catch {
    // 埋点失败不影响外跳
  }
}
