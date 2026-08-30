/**
 * GitHub 主语言 → 语言色点 token（token 定义在 global.css :root，F4 登记）。
 * 未收录语言回退 --lang-fallback，禁止在组件内裸写色值。
 */
const LANG_COLOR_TOKENS: Record<string, string> = {
  TypeScript: 'var(--lang-ts)',
  JavaScript: 'var(--lang-js)',
  Java: 'var(--lang-java)',
  Python: 'var(--lang-python)',
  Go: 'var(--lang-go)',
  Rust: 'var(--lang-rust)',
  Vue: 'var(--lang-vue)',
  HTML: 'var(--lang-html)',
  CSS: 'var(--lang-css)',
  SCSS: 'var(--lang-css)',
  Less: 'var(--lang-css)',
  C: 'var(--lang-c)',
  'C++': 'var(--lang-cpp)',
  'C#': 'var(--lang-cpp)',
  Shell: 'var(--lang-shell)',
  PowerShell: 'var(--lang-shell)',
  Batchfile: 'var(--lang-shell)',
  Kotlin: 'var(--lang-kotlin)',
  Swift: 'var(--lang-swift)',
  PHP: 'var(--lang-php)',
  Ruby: 'var(--lang-ruby)',
}

export function langColor(language: string | null): string {
  if (!language) return 'var(--lang-fallback)'
  return LANG_COLOR_TOKENS[language] ?? 'var(--lang-fallback)'
}

/** 相对更新时间（与终端风 `git log --date=relative` 一致的味道） */
export function relativeTime(iso: string | null): string {
  if (!iso) return '-'
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return '-'
  const diffMs = Date.now() - then
  if (diffMs < 0) return 'now'
  const minutes = Math.floor(diffMs / 60_000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}d ago`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months}mo ago`
  return `${Math.floor(months / 12)}y ago`
}
