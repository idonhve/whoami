/**
 * 后端 API 基址：前端静态站与后端分域部署（Render static + web service）时
 * 由构建期 VITE_API_BASE 注入；同源部署（docker compose / 本地开发）留空走相对路径。
 */

export const API_BASE = ((import.meta.env.VITE_API_BASE ?? '') as string).replace(/\/+$/, '')

/** 相对路径 → 带基址的完整请求地址 */
export function apiUrl(path: string): string {
  return `${API_BASE}${path}`
}
