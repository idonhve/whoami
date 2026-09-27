import { describe, expect, it } from 'vitest'

import type { GeoProvince } from '@/api/stats'
import chinaRaw from '@/assets/geo/china.json?raw'

import { buildChinaMapOption, buildMapSeriesData, buildScatterData, formatProvinceTooltip } from './chinaMapData'
import { extractGeoProvinceNames, parseChinaGeo } from './chinaGeo'

const FIXTURE: GeoProvince[] = [
  { province: '广东省', count: 12, cities: [{ city: '深圳市', count: 7 }, { city: '广州市', count: 5 }] },
  { province: '北京市', count: 8, cities: [{ city: '北京市', count: 8 }] },
  { province: '内蒙古自治区', count: 2, cities: [{ city: '呼和浩特市', count: 2 }] },
]

const GEO = parseChinaGeo(chinaRaw)
const GEO_NAMES = extractGeoProvinceNames(GEO)

describe('入库 GeoJSON（DataV 标准版图）', () => {
  it('为 FeatureCollection 且包含台湾省与九段线要素（adcode=100000_JD）', () => {
    expect(GEO.type).toBe('FeatureCollection')
    const names = GEO.features.map((f) => f.properties?.name)
    expect(names).toContain('台湾省')
    expect(names).toContain('香港特别行政区')
    expect(names).toContain('澳门特别行政区')
    const jd = GEO.features.find((f) => String(f.properties?.adcode) === '100000_JD')
    expect(jd).toBeDefined()
  })

  it('extractGeoProvinceNames 过滤无名 feature（南海诸岛 name 为空不入映射）', () => {
    expect(GEO_NAMES).toContain('广东省')
    expect(GEO_NAMES).not.toContain('')
    expect(GEO_NAMES).toHaveLength(34)
  })
})

describe('省级数据映射（geo 接口 → ECharts）', () => {
  it('buildMapSeriesData 只保留有访问数据的省份（境外/无数据不着色）', () => {
    const data = buildMapSeriesData(FIXTURE, [...GEO_NAMES])
    const guangdong = data.find((d) => d.name === '广东省')
    expect(guangdong?.value).toBe(12)
    // 无数据省份不进 series data
    expect(data.find((d) => d.name === '西藏自治区')).toBeUndefined()
    expect(data.length).toBe(3)
  })

  it('ip2region 无后缀省名可兜底前缀匹配（内蒙古 ↔ 内蒙古自治区）', () => {
    const data = buildMapSeriesData(
      [{ province: '内蒙古', count: 2, cities: [] }],
      ['内蒙古自治区'],
    )
    expect(data).toEqual([{ name: '内蒙古自治区', value: 2 }])
  })

  it('buildScatterData 打点在省会坐标，value 第三位为访问次数', () => {
    const scatter = buildScatterData(FIXTURE)
    const guangdong = scatter.find((s) => s.name === '广东省')
    expect(guangdong?.value).toEqual([113.28, 23.13, 12])
  })

  it('无省会坐标的记录跳过打点（不产出脏点）', () => {
    const scatter = buildScatterData([{ province: '境外', count: 3, cities: [] }])
    expect(scatter).toEqual([])
  })
})

describe('hover tooltip：该省访问 N 次 + 城市 TOP5', () => {
  it('显示省份、访问次数与城市列表', () => {
    const text = formatProvinceTooltip(
      { name: '广东省', data: { name: '广东省', value: 12 } },
      FIXTURE,
    )
    expect(text).toContain('广东省')
    expect(text).toContain('该省访问 12 次')
    expect(text).toContain('1. 深圳市 · 7')
    expect(text).toContain('2. 广州市 · 5')
  })

  it('无数据省份提示暂无访问数据', () => {
    const text = formatProvinceTooltip({ name: '西藏自治区' }, FIXTURE)
    expect(text).toContain('西藏自治区')
    expect(text).toContain('暂无访问数据')
  })

  it('输出转义防 XSS（省份名带 HTML 时被转义）', () => {
    const text = formatProvinceTooltip(
      { name: '<script>', data: { name: '<script>', value: 1 } },
      FIXTURE,
    )
    expect(text).toContain('&lt;script&gt;')
    expect(text).not.toContain('<script>')
  })
})

describe('ChinaMap option 构建', () => {
  const colors = {
    green: '#00ff9c',
    greenGlow: 'rgba(0,255,156,.35)',
    cyan: '#2bd9ff',
    magenta: '#ff2e88',
    amber: '#ffb800',
    text: '#c8d6e5',
    textDim: '#64788f',
    border: '#16222f',
    panel: '#0d151f',
    bg: '#090f16',
  }

  it('默认打点为 effectScatter 呼吸动效（本页唯一重动效锚点）', () => {
    const option = buildChinaMapOption({
      provinces: FIXTURE,
      geoNames: GEO_NAMES,
      colors,
      reducedMotion: false,
    })
    expect(option.animation).toBe(true)
    const scatter = option.series[1] as { type: string; coordinateSystem: string }
    expect(scatter.type).toBe('effectScatter')
    expect(scatter.coordinateSystem).toBe('geo')
  })

  it('reduced-motion 降级：animation 关闭、涟漪退化为静态 scatter（铁律）', () => {
    const option = buildChinaMapOption({
      provinces: FIXTURE,
      geoNames: GEO_NAMES,
      colors,
      reducedMotion: true,
    })
    expect(option.animation).toBe(false)
    const scatter = option.series[1] as { type: string }
    expect(scatter.type).toBe('scatter')
  })

  it('visualMap max 随数据收敛（单省 1 次也不至于除零）', () => {
    const option = buildChinaMapOption({
      provinces: [{ province: '北京市', count: 1, cities: [] }],
      geoNames: GEO_NAMES,
      colors,
      reducedMotion: true,
    })
    expect(option.visualMap.max).toBe(1)
  })
})
