import { afterEach, describe, expect, it, vi } from 'vitest'

import { isLowPowerDevice, resolveDegraded } from '@/composables/degrade'

function mockDevice(cores: number, width: number) {
  vi.stubGlobal('navigator', { ...navigator, hardwareConcurrency: cores })
  Object.defineProperty(window, 'innerWidth', { value: width, configurable: true })
}

describe('降级模式判定', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('硬件并发 ≤ 4 进入降级', () => {
    mockDevice(4, 1280)
    expect(isLowPowerDevice()).toBe(true)
  })

  it('视口宽 < 768 进入降级', () => {
    mockDevice(8, 375)
    expect(isLowPowerDevice()).toBe(true)
  })

  it('高配桌面不降级', () => {
    mockDevice(8, 1440)
    expect(isLowPowerDevice()).toBe(false)
  })

  it('degrade_force_full=true 时低配设备也强制满血', () => {
    mockDevice(2, 375)
    expect(resolveDegraded(true)).toBe(false)
  })

  it('degrade_force_full=false 时按设备判定', () => {
    mockDevice(2, 1280)
    expect(resolveDegraded(false)).toBe(true)
  })
})
