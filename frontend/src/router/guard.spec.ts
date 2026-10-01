import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { TOKEN_KEY } from '@/api/http'
import router from '@/router'

describe('admin 路由守卫', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('无 token 访问 /admin 跳转登录页并记录 redirect', async () => {
    await router.push('/admin')

    expect(router.currentRoute.value.name).toBe('admin-login')
    expect(router.currentRoute.value.query.redirect).toBe('/admin')
  })

  it('无 token 访问 /admin 子页同样跳转登录页', async () => {
    await router.push('/admin')

    expect(router.currentRoute.value.name).toBe('admin-login')
  })

  it('持有 token 时允许进入 /admin', async () => {
    localStorage.setItem(TOKEN_KEY, 'fake-token')

    await router.push('/admin')

    expect(router.currentRoute.value.name).toBe('admin-home')
  })

  it('登录页本身无需 token', async () => {
    await router.push('/admin/login')

    expect(router.currentRoute.value.name).toBe('admin-login')
  })

  it('前台页面不受守卫影响', async () => {
    await router.push('/')

    expect(router.currentRoute.value.name).toBe('home')
  })
})
