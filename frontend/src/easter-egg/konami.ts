/**
 * 键盘秘籍序列匹配器（Spec 11）：↑↑↓↓←→←→BA，连续 10 键，窗口期内完成。
 *
 * 纯状态机设计便于单测：createKonamiMatcher 可注入时间源，
 * press(key) 输入一个键返回匹配状态；非候选首键 O(1) 退出，不进入匹配状态。
 */
export const KONAMI_SEQUENCE = [
  'ArrowUp',
  'ArrowUp',
  'ArrowDown',
  'ArrowDown',
  'ArrowLeft',
  'ArrowRight',
  'ArrowLeft',
  'ArrowRight',
  'b',
  'a',
] as const

/** 序列窗口期：首键按下后需在此时间内完成整段，超时重置（Spec 11：约 5 秒） */
export const KONAMI_WINDOW_MS = 5000

export type KonamiState = 'idle' | 'matching' | 'completed'

export interface KonamiMatcher {
  /** 输入一个键，返回匹配后的状态（completed 表示整段完成） */
  press(key: string): KonamiState
  /** 已匹配的键数（0 = 未开始） */
  readonly progress: number
  /** 手动重置（触发完成后也会自动重置，可再次触发） */
  reset(): void
}

/**
 * 创建序列匹配器。
 * @param now 时间源，默认 Date.now()；注入假时钟用于窗口期单测。
 */
export function createKonamiMatcher(now: () => number = () => Date.now()): KonamiMatcher {
  let progress = 0
  let startedAt = 0

  function expired(): boolean {
    return progress > 0 && now() - startedAt > KONAMI_WINDOW_MS
  }

  return {
    press(key: string): KonamiState {
      // 单字符键大小写不敏感（Shift / 大写锁定下的 B A 同样命中）
      const k = key.length === 1 ? key.toLowerCase() : key
      if (expired()) progress = 0
      if (progress === 0) {
        // O(1) 退出：非候选首键（↑）不进入匹配状态
        if (k !== KONAMI_SEQUENCE[0]) return 'idle'
        progress = 1
        startedAt = now()
        return 'matching'
      }
      if (k !== KONAMI_SEQUENCE[progress]) {
        // 错误序列中途重置
        progress = 0
        return 'idle'
      }
      progress += 1
      if (progress === KONAMI_SEQUENCE.length) {
        // 触发后重置状态，可再次触发（Spec 11：秘技可重复）
        progress = 0
        return 'completed'
      }
      return 'matching'
    },
    get progress() {
      return progress
    },
    reset(): void {
      progress = 0
    },
  }
}
