import { describe, expect, it } from 'vitest'

import { buildBootLogs } from '@/views/boot/bootLogs'

describe('开机伪日志文案', () => {
  it('日志行数 ≥ 5', () => {
    expect(buildBootLogs('localhost').length).toBeGreaterThanOrEqual(5)
  })

  it('首行注入域名文案', () => {
    const lines = buildBootLogs('localhost')
    expect(lines[0].text).toBe('> initializing localhost ...')
  })

  it('契约：site_config.domain 变化后日志文案随之变化', () => {
    const a = buildBootLogs('localhost')[0].text
    const b = buildBootLogs('example.dev')[0].text
    expect(a).not.toBe(b)
    expect(b).toContain('example.dev')
  })
})
