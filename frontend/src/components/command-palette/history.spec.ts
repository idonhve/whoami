import { beforeEach, describe, expect, it } from 'vitest'

import { HISTORY_KEY, HISTORY_MAX, loadHistory, pushHistory } from '@/components/command-palette/history'

describe('命令历史（Spec 10）', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('空存储读取返回空数组', () => {
    expect(loadHistory()).toEqual([])
  })

  it('损坏 JSON 静默返回空数组，不抛错', () => {
    localStorage.setItem(HISTORY_KEY, '{oops')
    expect(loadHistory()).toEqual([])
  })

  it('非字符串数组过滤后返回', () => {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(['works', 42, null, 'tech']))
    expect(loadHistory()).toEqual(['works', 'tech'])
  })

  it('记录命令：最新在前', () => {
    pushHistory('works')
    pushHistory('tech')
    expect(loadHistory()).toEqual(['tech', 'works'])
  })

  it('去重：重复执行移到最前，不重复出现', () => {
    pushHistory('works')
    pushHistory('tech')
    pushHistory('works')
    expect(loadHistory()).toEqual(['works', 'tech'])
  })

  it('上限 5 条，超出截断最旧的', () => {
    for (const cmd of ['a', 'b', 'c', 'd', 'e', 'f', 'g']) pushHistory(cmd)
    expect(loadHistory()).toEqual(['g', 'f', 'e', 'd', 'c'])
    expect(loadHistory()).toHaveLength(HISTORY_MAX)
  })

  it('空命令不记录', () => {
    pushHistory('   ')
    expect(loadHistory()).toEqual([])
  })
})
