import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory, type Router } from 'vue-router'

/**
 * 埋点 SDK 行为契约（Spec 05 Testing Decisions）：
 * 不测 sendBeacon 实现细节，测调用契约——URL、payload、触发时机。
 * vi.resetModules 保证每个用例拿到全新模块实例（installTracker 幂等 flag 归零）。
 */

interface BeaconCall {
  url: string
  blob: Blob
}

let beaconSpy: ReturnType<typeof vi.fn>
let beaconCalls: BeaconCall[]

/** 安装 sendBeacon mock：同步记录调用，payload 由断言处异步解析 */
function installBeaconMock(): void {
  beaconCalls = []
  beaconSpy = vi.fn((url: string, blob: Blob) => {
    beaconCalls.push({ url, blob })
    return true
  })
  Object.defineProperty(navigator, 'sendBeacon', {
    value: beaconSpy,
    configurable: true,
  })
}

async function payloadOf(call: BeaconCall): Promise<Record<string, unknown>> {
  return JSON.parse(await call.blob.text())
}

/** 当前用例使用的模块实例（afterEach 用它移除本用例注册的全局监听器） */
let currentTracker: (typeof import('./index')) | null = null

async function freshTracker() {
  vi.resetModules()
  currentTracker = await import('./index')
  return currentTracker
}

function makeRouter(): Router {
  // happy-dom 同文件用例间共享 window：先归位到 /，保证每个用例从首页开始
  window.history.replaceState(null, '', '/')
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/about', name: 'about', component: { template: '<div />' } },
      { path: '/works', name: 'works', component: { template: '<div />' } },
    ],
  })
}

beforeEach(() => {
  sessionStorage.clear()
  installBeaconMock()
})

afterEach(async () => {
  // 移除当前用例模块实例挂的全局监听器，避免用例间 pagehide 窜扰
  currentTracker?.resetTrackerForTest()
  currentTracker = null
  vi.unstubAllGlobals()
})

describe('会话开始（POST /api/track/session）', () => {
  it('installTracker 安装即上报会话开始：sessionId 36 位 + entryPage', async () => {
    const { installTracker } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    await router.push('/')

    const startCalls = beaconCalls.filter((c) => c.url === '/api/track/session')
    expect(startCalls).toHaveLength(1)
    const payload = await payloadOf(startCalls[0])
    expect(payload.sessionId).toMatch(/^[0-9a-f-]{36}$/)
    expect(payload.entryPage).toBe('/')
  })

  it('sessionId 复用 sessionStorage 的 whoami:sid（跨模块约定）', async () => {
    sessionStorage.setItem('whoami:sid', '11111111-2222-3333-4444-555555555555')
    const { installTracker, SESSION_KEY } = await freshTracker()
    expect(SESSION_KEY).toBe('whoami:sid')
    const router = makeRouter()
    installTracker(router)
    await router.push('/')

    const startCalls = beaconCalls.filter((c) => c.url === '/api/track/session')
    const payload = await payloadOf(startCalls[0])
    expect(payload.sessionId).toBe('11111111-2222-3333-4444-555555555555')
  })
})

describe('路由变化自动 page_view（POST /api/track/event）', () => {
  it('初始导航完成触发一次 page_view（URL 与 payload 契约）', async () => {
    const { installTracker } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    await router.push('/')

    const views = beaconCalls.filter((c) => c.url === '/api/track/event')
    expect(views).toHaveLength(1)
    const payload = await payloadOf(views[0])
    expect(payload.eventType).toBe('page_view')
    expect(payload.pagePath).toBe('/')
    expect(payload.sessionId).toMatch(/^[0-9a-f-]{36}$/)
  })

  it('路由切换到 /about 再触发一次 page_view（pagePath 随路由）', async () => {
    const { installTracker } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    await router.push('/')
    await router.push('/about')

    const views = beaconCalls.filter((c) => c.url === '/api/track/event')
    expect(views).toHaveLength(2)
    const payload = await payloadOf(views[1])
    expect(payload.eventType).toBe('page_view')
    expect(payload.pagePath).toBe('/about')
  })

  it('installTracker 幂等：重复安装不产生重复上报', async () => {
    const { installTracker } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    installTracker(router)
    await router.push('/')

    const views = beaconCalls.filter((c) => c.url === '/api/track/event')
    expect(views).toHaveLength(1)
  })
})

describe('会话结束（POST /api/track/session/{sid}/end）', () => {
  it('pagehide 触发 session end 上报，带 lastPagePath', async () => {
    const { installTracker } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    await router.push('/')
    await router.push('/about')

    window.dispatchEvent(new Event('pagehide'))

    const endCalls = beaconCalls.filter((c) => c.url.includes('/end'))
    expect(endCalls).toHaveLength(1)
    expect(endCalls[0].url).toBe(
      `/api/track/session/${sessionStorage.getItem('whoami:sid')}/end`,
    )
    const payload = await payloadOf(endCalls[0])
    expect(payload.lastPagePath).toBe('/about')
  })

  it('visibilitychange hidden 时同样触发 session end', async () => {
    const { installTracker } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    await router.push('/')

    Object.defineProperty(document, 'visibilityState', {
      value: 'hidden',
      configurable: true,
    })
    document.dispatchEvent(new Event('visibilitychange'))

    const endCalls = beaconCalls.filter((c) => c.url.includes('/end'))
    expect(endCalls).toHaveLength(1)
  })
})

describe('统一事件入口 trackEvent', () => {
  it('trackEvent(eventType, detail) 走 /api/track/event，payload 带 detail', async () => {
    const { installTracker, trackEvent } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    await router.push('/')

    trackEvent('cmd_palette_use', { command: 'about' })

    const events = beaconCalls.filter(
      (c) => c.url === '/api/track/event' && c.blob.size > 0,
    )
    const payloads = await Promise.all(events.map(payloadOf))
    const hit = payloads.find((p) => p.eventType === 'cmd_palette_use')
    expect(hit).toBeDefined()
    expect(hit?.detail).toEqual({ command: 'about' })
  })

  it('detail 缺省时 payload 不含 detail 字段', async () => {
    const { installTracker, trackEvent } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    await router.push('/')

    trackEvent('page_view')

    const views = beaconCalls.filter((c) => c.url === '/api/track/event')
    expect(views.length).toBeGreaterThanOrEqual(1)
    const payload = await payloadOf(views[views.length - 1])
    expect(payload).not.toHaveProperty('detail')
  })
})

describe('sendBeacon 优先与降级', () => {
  it('sendBeacon 不可用时退回 keepalive fetch', async () => {
    Object.defineProperty(navigator, 'sendBeacon', { value: undefined, configurable: true })
    const fetchMock = vi.fn().mockResolvedValue({ ok: true })
    vi.stubGlobal('fetch', fetchMock)

    const { installTracker, trackEvent } = await freshTracker()
    const router = makeRouter()
    installTracker(router)
    trackEvent('page_view')

    await vi.waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/track/event',
        expect.objectContaining({ method: 'POST', keepalive: true }),
      )
    })
  })

  it('sendBeacon 抛异常不影响调用方（静默）', async () => {
    Object.defineProperty(navigator, 'sendBeacon', {
      value: vi.fn(() => {
        throw new Error('beacon down')
      }),
      configurable: true,
    })
    const { trackEvent } = await freshTracker()
    expect(() => trackEvent('easter_egg', { type: 'konami' })).not.toThrow()
  })
})
