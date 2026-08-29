import { http } from './http'

export interface LoginResponse {
  token: string
  expiresIn: number
}

export interface AdminInfo {
  id: number
  username: string
}

export function loginApi(username: string, password: string) {
  return http.post<LoginResponse>('/admin/api/auth/login', { username, password })
}

export function refreshApi() {
  return http.post<LoginResponse>('/admin/api/auth/refresh')
}

export function meApi() {
  return http.get<AdminInfo>('/admin/api/auth/me')
}
