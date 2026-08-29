import type { ApiResult } from '@/types/api'

export const TOKEN_KEY = 'whoami_admin_token'

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(options.headers as Record<string, string>) }
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(path, { ...options, headers })

  let body: ApiResult<T> | null = null
  try {
    body = (await response.json()) as ApiResult<T>
  } catch {
    // 非 JSON 响应按失败处理
  }

  if (!response.ok || !body || body.code !== 0) {
    if (response.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      const { default: router } = await import('@/router')
      const current = router.currentRoute.value
      if (current.path.startsWith('/admin')) {
        router.push({ name: 'admin-login', query: { redirect: current.fullPath } })
      }
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
