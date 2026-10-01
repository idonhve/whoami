/**
 * 命令历史（Spec 10）：localStorage 键 'whoami:cmd-history'。
 * 最多 5 条、去重、最新在前。
 */

export const HISTORY_KEY = 'whoami:cmd-history'
export const HISTORY_MAX = 5

/** 读取历史（损坏/缺失返回空数组，不抛错） */
export function loadHistory(): string[] {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item): item is string => typeof item === 'string')
  } catch {
    return []
  }
}

/** 写入历史（调用方保证已是合法数组） */
function persist(items: string[]): void {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(items))
}

/** 记录一条命令：去重后插到最前，截断到上限 */
export function pushHistory(command: string): string[] {
  const q = command.trim()
  if (!q) return loadHistory()
  const next = [q, ...loadHistory().filter((item) => item !== q)].slice(0, HISTORY_MAX)
  persist(next)
  return next
}
