import { afterEach, describe, expect, it, vi } from 'vitest'

import { defaultSiteConfig, fetchSiteConfig } from '@/api/siteConfig'

function mockApiResponse(data: unknown, ok = true) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok,
      status: ok ? 200 : 500,
      json: () => Promise.resolve(ok ? { code: 0, data } : null),
    }),
  )
}

describe('站点配置 API', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('映射公开白名单键并转型', async () => {
    mockApiResponse({
      domain: 'example.dev',
      ownerName: '张三',
      githubUrl: 'https://github.com/x',
      degradeForceFull: 'true',
    })

    const config = await fetchSiteConfig()
    expect(config.domain).toBe('example.dev')
    expect(config.ownerName).toBe('张三')
    expect(config.githubUrl).toBe('https://github.com/x')
    expect(config.degradeForceFull).toBe(true)
  })

  it('接口失败时回退默认值（域名占位 localhost）', async () => {
    mockApiResponse(null, false)
    const config = await fetchSiteConfig()
    expect(config).toEqual(defaultSiteConfig)
    expect(config.domain).toBe('localhost')
  })

  it('网络异常时回退默认值', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network down')))
    const config = await fetchSiteConfig()
    expect(config).toEqual(defaultSiteConfig)
  })

  it('缺字段时逐字段回退默认', async () => {
    mockApiResponse({ domain: 'only-domain.dev' })
    const config = await fetchSiteConfig()
    expect(config.domain).toBe('only-domain.dev')
    expect(config.ownerName).toBe(defaultSiteConfig.ownerName)
    expect(config.degradeForceFull).toBe(false)
  })
})
