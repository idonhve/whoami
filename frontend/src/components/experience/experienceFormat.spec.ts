import { describe, expect, it } from 'vitest'

import { defaultSummary, formatDateRange, parseCounter, validateRadar } from './experienceFormat'

describe('parseCounter 战果数值解析', () => {
  it('百分比：300% → 数字 300 + 后缀 %', () => {
    expect(parseCounter('300%')).toEqual({ num: 300, suffix: '%' })
  })

  it('带单位：50w+ → 数字 50 + 后缀 w+', () => {
    expect(parseCounter('50w+')).toEqual({ num: 50, suffix: 'w+' })
  })

  it('小数：3.5x → 数字 3.5 + 后缀 x', () => {
    expect(parseCounter('3.5x')).toEqual({ num: 3.5, suffix: 'x' })
  })

  it('纯文本（无前导数字）返回 num=null，原样显示', () => {
    expect(parseCounter('主导核心重构')).toEqual({ num: null, suffix: '主导核心重构' })
  })

  it('负数 -8% 正常解析', () => {
    expect(parseCounter('-8%')).toEqual({ num: -8, suffix: '%' })
  })
})

describe('defaultSummary 默认态摘要（≤30 字守门）', () => {
  const base = {
    company: '星云科技',
    title: '资深前端工程师',
    startDate: '2021-06-01',
    endDate: null,
  }
  it('拼接公司 · 职位 · 时间，且字符数 ≤ 30', () => {
    const summary = defaultSummary(base)
    expect(summary.length).toBeLessThanOrEqual(30)
    expect(summary).toContain('星云科技')
    expect(summary).toContain('资深前端工程师')
    expect(summary).toContain('至今')
  })

  it('超长文案被截断兜底（仍 ≤ 30）', () => {
    const long = {
      company: '非常非常非常非常长的公司全称股份有限公司',
      title: '极其极其极其极其长的高级专家工程师职位名称',
      startDate: '2018-01-01',
      endDate: '2026-01-01',
    }
    const summary = defaultSummary(long)
    expect(summary.length).toBeLessThanOrEqual(30)
  })
})

describe('formatDateRange 日期区间', () => {
  it('空 endDate 显示至今', () => {
    expect(formatDateRange('2021-06-01', null)).toBe('2021.06 – 至今')
  })
  it('有 endDate 显示两端', () => {
    expect(formatDateRange('2021-06-01', '2023-12-31')).toBe('2021.06 – 2023.12')
  })
})

describe('validateRadar 雷达维度校验（3~8 维）', () => {
  it('2 维不通过', () => {
    expect(
      validateRadar([
        { dimension: '架构', score: 80 },
        { dimension: '工程', score: 70 },
      ]),
    ).toMatch(/3~8/)
  })
  it('9 维不通过', () => {
    const d = Array.from({ length: 9 }, (_, i) => ({ dimension: `维度${i}`, score: 50 }))
    expect(validateRadar(d)).toMatch(/3~8/)
  })
  it('维度名重复不通过', () => {
    expect(
      validateRadar([
        { dimension: '沟通', score: 80 },
        { dimension: '沟通', score: 70 },
        { dimension: '协作', score: 60 },
      ]),
    ).toMatch(/重复/)
  })
  it('维度名超长不通过', () => {
    expect(
      validateRadar([
        { dimension: '这是一个长度超过二十个字符的维度名称超长测', score: 80 },
        { dimension: 'b', score: 70 },
        { dimension: 'c', score: 60 },
      ]),
    ).toMatch(/最长 20/)
  })
  it('score 越界不通过', () => {
    expect(
      validateRadar([
        { dimension: 'a', score: 101 },
        { dimension: 'b', score: 70 },
        { dimension: 'c', score: 60 },
      ]),
    ).toMatch(/0~100/)
  })
  it('合法 4 维通过', () => {
    expect(
      validateRadar([
        { dimension: '架构', score: 88 },
        { dimension: '沟通', score: 92 },
        { dimension: '工程', score: 76 },
        { dimension: '产品', score: 84 },
      ]),
    ).toBeNull()
  })
})
