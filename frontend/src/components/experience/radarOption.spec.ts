import { describe, expect, it } from 'vitest'

import { buildRadarOption } from './radarOption'

const palette = {
  text: '#c8d6e5',
  textDim: '#64788f',
  border: '#16222f',
  background: '#090f16',
  green: '#00ff9c',
  greenGlow: 'rgba(0,255,156,0.35)',
}

describe('buildRadarOption 雷达图维度渲染', () => {
  it('indicator 数量 = radar 维度数（3~8），name 依次为维度名', () => {
    const radar = [
      { dimension: '架构', score: 88 },
      { dimension: '沟通', score: 92 },
      { dimension: '工程', score: 76 },
      { dimension: '产品', score: 84 },
    ]
    const option = buildRadarOption(radar, palette)
    const radarOption = option.radar
    expect(Array.isArray(radarOption)).toBe(false)
    const indicator = (option.radar as { indicator: { name: string }[] }).indicator
    expect(indicator).toHaveLength(4)
    expect(indicator.map((i) => i.name)).toEqual(['架构', '沟通', '工程', '产品'])
  })

  it('series 数值 = 各维度 score', () => {
    const radar = [
      { dimension: 'a', score: 40 },
      { dimension: 'b', score: 70 },
      { dimension: 'c', score: 95 },
    ]
    const option = buildRadarOption(radar, palette)
    const series = option.series as [{ data: [{ value: number[] }] }]
    expect(series[0].data[0].value).toEqual([40, 70, 95])
  })
})
