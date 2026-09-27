/**
 * 埋点上报传输层（Spec 05）：sendBeacon 优先（页面卸载也不丢包、不阻塞），
 * 不可用或入队失败时退回 keepalive fetch。任何失败一律静默——埋点绝不影响业务。
 */

/** sendBeacon 优先的 JSON 上报；失败静默 */
export function sendBeaconJson(url: string, payload: unknown): void {
  try {
    const body = JSON.stringify(payload)
    if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
      const queued = navigator.sendBeacon(url, new Blob([body], { type: 'application/json' }))
      if (queued) return
    }
    void fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      keepalive: true,
    }).catch(() => {})
  } catch {
    // 埋点失败静默
  }
}
