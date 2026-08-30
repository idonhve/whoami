/**
 * 读取设计 token（global.css :root）的运行时值。
 * 供 Canvas / WebGL 等无法直接引用 CSS 变量的场景使用，保证颜色唯一来源仍是 token。
 */
export function cssVar(name: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}
