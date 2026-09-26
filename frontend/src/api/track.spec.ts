import { afterEach, describe, expect, it, vi } from 'vitest'

import { trackGithubOutbound } from '@/api/track'

describe('github_outbound 埋点上报', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('点击外跳后向 /api/track/event 上报 github_outbound（sendBeacon 优先）', () => {
    const beaconSpy = vi.fn().mockReturnValue(true)
    Object.defineProperty(navigator, 'sendBeacon', {
      value: beaconSpy,
      configurable: true,
    })

    trackGithubOutbound('whoami-site')

    expect(beaconSpy).toHaveBeenCalledTimes(1)
    const [url, blob] = beaconSpy.mock.calls[0]
    expect(url).toBe('/api/track/event')
    expect(blob).toBeInstanceOf(Blob)
    // payload 在 Blob 内，异步读取断言
    return blob.text().then((text: string) => {
      const payload = JSON.parse(text)
      expect(payload.eventType).toBe('github_outbound')
      expect(payload.detail).toBe('whoami-site')
      expect(payload.pagePath).toBe('/')
      expect(payload.sessionId).toMatch(/^[0-9a-f-]{36}$/)
    })
  })

  it('无 sendBeacon 时退回 keepalive fetch，失败静默', async () => {
    Object.defineProperty(navigator, 'sendBeacon', {
      value: undefined,
      configurable: true,
    })
    const fetchMock = vi.fn().mockResolvedValue({ ok: false })
    vi.stubGlobal('fetch', fetchMock)

    trackGithubOutbound('cli-tool')

    await vi.waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/track/event',
        expect.objectContaining({ method: 'POST', keepalive: true }),
      )
    })
  })

  it('埋点异常不外抛（外跳不受影响）', () => {
    Object.defineProperty(navigator, 'sendBeacon', {
      value: vi.fn(() => {
        throw new Error('beacon down')
      }),
      configurable: true,
    })

    expect(() => trackGithubOutbound('repo')).not.toThrow()
  })
})
