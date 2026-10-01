/**
 * 开机动画会话状态（Spec 01）：
 * - whoami:boot:seen  24h 内二次访问跳过开机动画（存时间戳，过期自动失效）
 * - whoami:boot:muted 打字音效静音偏好，持久化记住选择
 */

export const BOOT_SEEN_KEY = 'whoami:boot:seen'
export const BOOT_MUTED_KEY = 'whoami:boot:muted'
export const BOOT_SEEN_TTL_MS = 24 * 60 * 60 * 1000

export function shouldPlayBoot(now: number = Date.now()): boolean {
  const raw = localStorage.getItem(BOOT_SEEN_KEY)
  if (!raw) return true
  const seenAt = Number(raw)
  if (!Number.isFinite(seenAt)) return true
  return now - seenAt >= BOOT_SEEN_TTL_MS
}

export function markBootSeen(now: number = Date.now()): void {
  localStorage.setItem(BOOT_SEEN_KEY, String(now))
}

export function loadBootMuted(): boolean {
  return localStorage.getItem(BOOT_MUTED_KEY) === 'true'
}

export function saveBootMuted(muted: boolean): void {
  localStorage.setItem(BOOT_MUTED_KEY, String(muted))
}
