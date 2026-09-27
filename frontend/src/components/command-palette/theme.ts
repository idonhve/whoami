/**
 * theme 命令的全局强调色切换（Spec 10）：覆盖 --green 系列 token 派生色。
 * 预设三档 green / cyan / magenta，全部由既有 token 派生（var(--cyan) / color-mix），
 * 不引入新色值；持久化 localStorage 键 'whoami:theme'。
 * 通过 <html data-cmd-theme="..."> 属性选择器生效（注入一次 style 标签，konami-toast 同款模式）。
 */

export const THEME_KEY = 'whoami:theme'

export const THEME_PRESETS = ['green', 'cyan', 'magenta'] as const

export type ThemePreset = (typeof THEME_PRESETS)[number]

const STYLE_ID = 'whoami-cmd-theme-style'
const THEME_ATTR = 'data-cmd-theme'

function ensureThemeStyle(): void {
  if (document.getElementById(STYLE_ID)) return
  const style = document.createElement('style')
  style.id = STYLE_ID
  style.textContent = `
html[data-cmd-theme='cyan'] {
  --green: var(--cyan);
  --green-soft: var(--cyan-soft);
  --green-glow: color-mix(in srgb, var(--cyan) 35%, transparent);
}
html[data-cmd-theme='magenta'] {
  --green: var(--magenta);
  --green-soft: color-mix(in srgb, var(--magenta) 12%, transparent);
  --green-glow: color-mix(in srgb, var(--magenta) 35%, transparent);
}`
  document.head.appendChild(style)
}

function isPreset(value: unknown): value is ThemePreset {
  return typeof value === 'string' && (THEME_PRESETS as readonly string[]).includes(value)
}

/** 应用主题预设（green 档移除属性，回到 :root 默认） */
export function applyTheme(theme: ThemePreset): void {
  ensureThemeStyle()
  if (theme === 'green') {
    document.documentElement.removeAttribute(THEME_ATTR)
  } else {
    document.documentElement.setAttribute(THEME_ATTR, theme)
  }
  localStorage.setItem(THEME_KEY, theme)
}

/** 启动时恢复上次主题（非法值静默回退 green）；在 App setup 期调用避免闪烁 */
export function initTheme(): ThemePreset {
  const stored = localStorage.getItem(THEME_KEY)
  const theme = isPreset(stored) ? stored : 'green'
  applyTheme(theme)
  return theme
}

/** 切换到下一档（theme 命令），返回切换后的预设 */
export function cycleTheme(current: ThemePreset): ThemePreset {
  const index = THEME_PRESETS.indexOf(current)
  const next = THEME_PRESETS[(index + 1) % THEME_PRESETS.length]
  applyTheme(next)
  return next
}
