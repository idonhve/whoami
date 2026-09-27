import { describe, expect, it } from 'vitest'

import { hasMatch, matchCommands } from '@/components/command-palette/fuzzyMatch'

const COMMANDS = [
  { name: 'home' },
  { name: 'works' },
  { name: 'tech' },
  { name: 'experience' },
  { name: 'awards' },
  { name: 'help' },
]

describe('命令模糊匹配（Spec 10）', () => {
  it('空输入返回全量命令', () => {
    expect(matchCommands('', COMMANDS)).toHaveLength(COMMANDS.length)
    expect(matchCommands('   ', COMMANDS)).toHaveLength(COMMANDS.length)
  })

  it('前缀命中：ex → experience', () => {
    const result = matchCommands('ex', COMMANDS)
    expect(result[0]?.name).toBe('experience')
  })

  it('子序列命中：wrk → works（非连续子串）', () => {
    const result = matchCommands('wrk', COMMANDS)
    expect(result.map((c) => c.name)).toContain('works')
  })

  it('expr → experience（子序列）', () => {
    const result = matchCommands('expr', COMMANDS)
    expect(result.map((c) => c.name)).toContain('experience')
  })

  it('无命中返回空数组，hasMatch 为 false', () => {
    expect(matchCommands('zzz', COMMANDS)).toHaveLength(0)
    expect(hasMatch('zzz', COMMANDS)).toBe(false)
    expect(hasMatch('wrk', COMMANDS)).toBe(true)
  })

  it('匹配大小写不敏感', () => {
    const result = matchCommands('WORKS', COMMANDS)
    expect(result[0]?.name).toBe('works')
  })
})
