import type { ApiResult } from '@/types/api'

export const TOKEN_KEY = 'whoami_admin_token'

const REFRESH_PATH = '/admin/api/auth/refresh'
const LOGIN_PATH = '/admin/api/auth/login'

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

/** 进行中的续签请求（并发 401 只续签一次，其余等待同一 Promise） */
let refreshPromise: Promise<boolean> | null = null

/**
 * 用当前（可能刚过期的）token 尝试续签。
 * 成功：新 token 写入 localStorage，返回 true；失败返回 false（由调用方踢回登录页）。
 */
function tryRefreshToken(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const token = localStorage.getItem(TOKEN_KEY)
      if (!token) return false
      try {
        const response = await fetch(REFRESH_PATH, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!response.ok) return false
        const body = (await response.json()) as ApiResult<{ token: string }> | null
        if (!body || body.code !== 0 || !body.data?.token) return false
        localStorage.setItem(TOKEN_KEY, body.data.token)
        return true
      } catch {
        return false
      }
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

/** 续签也救不回来：清会话、同步 store，当前在后台则跳登录页 */
async function handleSessionExpired() {
  localStorage.removeItem(TOKEN_KEY)
  const [{ default: router }, { useAuthStore }] = await Promise.all([
    import('@/router'),
    import('@/stores/auth'),
  ])
  useAuthStore().logout()
  const current = router.currentRoute.value
  if (current.path.startsWith('/admin') && current.name !== 'admin-login') {
    router.push({ name: 'admin-login', query: { redirect: current.fullPath } })
  }
}

function shouldAttemptRefresh(path: string) {
  // 只对后台业务接口续签；登录/续签接口本身的 401 直接走失败逻辑
  return path.startsWith('/admin/api/') && path !== REFRESH_PATH && path !== LOGIN_PATH
}

async function doFetch(path: string, options: RequestInit): Promise<Response> {
  const headers: Record<string, string> = { ...(options.headers as Record<string, string>) }
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  return fetch(path, { ...options, headers })
}

async function request<T>(path: string, options: RequestInit = {}, allowRefreshRetry = true): Promise<T> {
  const response = await doFetch(path, options)

  let body: ApiResult<T> | null = null
  try {
    body = (await response.json()) as ApiResult<T>
  } catch {
    // 非 JSON 响应按失败处理
  }

  // 401 → 先尝试续签一次，成功则以新 token 原样重试（用户无感知）
  if (response.status === 401 && allowRefreshRetry && shouldAttemptRefresh(path)) {
    if (await tryRefreshToken()) {
      return request<T>(path, options, false)
    }
  }

  if (!response.ok || !body || body.code !== 0) {
    if (response.status === 401) {
      await handleSessionExpired()
    }
    throw new ApiError(
      response.status,
      body?.code ?? response.status,
      body?.message ?? `请求失败（HTTP ${response.status}）`,
    )
  }

  return body.data as T
}

export const http = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: data === undefined ? undefined : JSON.stringify(data),
    }),
  put: <T>(path: string, data?: unknown) =>
    request<T>(path, {
      method: 'PUT',
      body: data === undefined ? undefined : JSON.stringify(data),
    }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
