import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createApp, h, nextTick, type App as VueApp } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'

import { TOKEN_KEY } from '@/api/http'
import { meApi } from '@/api/auth'
import FloatingAdminButton from './FloatingAdminButton.vue'

vi.mock('@/api/auth', () => ({
  meApi: vi.fn(),
  loginApi: vi.fn(),
  refreshApi: vi.fn(),
}))

const mockedMeApi = vi.mocked(meApi)

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/admin', name: 'admin-home', component: { template: '<div />' } },
    ],
  })
}

async function mountButton(path = '/', waitForMe = false) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = makeRouter()
  await router.push(path)
  const el = document.createElement('div')
  document.body.appendChild(el)
  const app: VueApp = createApp({ render: () => h(FloatingAdminButton) })
  app.use(pinia)
  app.use(router)
  app.mount(el)
  if (waitForMe) {
    // 等 watch(immediate) 里的 fetchMe 链路跑完
    await vi.waitFor(() => {
      expect(mockedMeApi.mock.calls.length).toBeGreaterThan(0)
    })
  }
  await nextTick()
  await nextTick()
  return { el, app }
}

describe('前台悬浮管理按钮', () => {
  beforeEach(() => {
    localStorage.clear()
    mockedMeApi.mockReset()
    document.body.innerHTML = ''
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('持有 token 且 me 校验通过 → 前台显示', async () => {
    localStorage.setItem(TOKEN_KEY, 'valid-token')
    mockedMeApi.mockResolvedValue({ id: 1, username: 'admin' })

    const { el } = await mountButton('/', true)

    await vi.waitFor(() => {
      expect(el.querySelector('.floating-admin-btn')).toBeTruthy()
    })
  })

  it('无 token（访客）→ 不渲染，也不发起 me 校验', async () => {
    const { el } = await mountButton('/')

    expect(mockedMeApi).not.toHaveBeenCalled()
    expect(el.querySelector('.floating-admin-btn')).toBeNull()
  })

  it('me 校验失败（token 失效）→ 不渲染', async () => {
    localStorage.setItem(TOKEN_KEY, 'stale-token')
    mockedMeApi.mockRejectedValue(new Error('401'))

    const { el } = await mountButton('/', true)

    expect(el.querySelector('.floating-admin-btn')).toBeNull()
  })

  it('后台页面自身不显示悬浮按钮', async () => {
    localStorage.setItem(TOKEN_KEY, 'valid-token')
    mockedMeApi.mockResolvedValue({ id: 1, username: 'admin' })

    const { el } = await mountButton('/admin', true)

    expect(el.querySelector('.floating-admin-btn')).toBeNull()
  })
})
