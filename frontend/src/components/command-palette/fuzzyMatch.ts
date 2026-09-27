/**
 * 命令模糊匹配（Spec 10）：前缀优先，退化为子序列匹配（wrk → works）。
 * 只做前缀 + 子序列，不做语义搜索（Out of Scope）。
 */

export interface MatchableCommand {
  name: string
}

/** input 是否为 name 的（不连续）子序列：按字符顺序贪心前进 */
function isSubsequence(input: string, name: string): boolean {
  let i = 0
  for (const ch of name) {
    if (ch === input[i]) i++
    if (i === input.length) return true
  }
  return i === input.length
}

/**
 * 过滤命令：空输入返回全量；
 * 前缀命中的排前面，子序列命中的排后面，都不命中则不返回该命令。
 */
export function matchCommands<T extends MatchableCommand>(input: string, commands: T[]): T[] {
  const q = input.trim().toLowerCase()
  if (!q) return [...commands]
  const prefixed: T[] = []
  const fuzzy: T[] = []
  for (const cmd of commands) {
    const name = cmd.name.toLowerCase()
    if (name.startsWith(q)) prefixed.push(cmd)
    else if (isSubsequence(q, name)) fuzzy.push(cmd)
  }
  return [...prefixed, ...fuzzy]
}

/** 是否存在任一命中（用于 command not found 判定） */
export function hasMatch<T extends MatchableCommand>(input: string, commands: T[]): boolean {
  return matchCommands(input, commands).length > 0
}
