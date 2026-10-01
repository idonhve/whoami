import { describe, expect, it } from 'vitest'

import { API_BASE, apiUrl } from '@/api/base'

describe('API 基址拼接（分域部署支持）', () => {
  it('测试环境未注入 VITE_API_BASE → 基址为空，路径原样返回', () => {
    expect(API_BASE).toBe('')
    expect(apiUrl('/api/site-config')).toBe('/api/site-config')
  })
})
