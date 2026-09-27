import type { GeoProvince } from '@/api/stats'

/**
 * 访客地图数据映射（Spec 05，纯函数便于单测）：
 * - 后端 geo 省名为 ip2region 原文口径（如"广东省"，与 DataV GeoJSON properties.name 一致）；
 *   兜底支持"内蒙古"这类无行政后缀写法的前缀匹配。
 * - 打点按省聚合在省会坐标（effectScatter 呼吸动效 = 本页唯一重动效锚点）。
 * - 境外 / 无数据省份不进 series data（不着色、不打点）。
 * - ECharts 无法消费 CSS 变量，颜色经 readCssVar 从 :root 读取（铁律：组件不写裸 hex）。
 */

/** 省会 / 特别行政区中心坐标（WGS84，供省级打点定位） */
export const PROVINCE_CENTERS: Record<string, [number, number]> = {
  北京市: [116.405, 39.905],
  天津市: [117.19, 39.13],
  河北省: [114.5, 38.04],
  山西省: [112.55, 37.87],
  内蒙古自治区: [111.67, 40.82],
  辽宁省: [123.43, 41.8],
  吉林省: [125.32, 43.9],
  黑龙江省: [126.53, 45.8],
  上海市: [121.47, 31.23],
  江苏省: [118.78, 32.06],
  浙江省: [120.15, 30.28],
  安徽省: [117.28, 31.86],
  福建省: [119.3, 26.08],
  江西省: [115.89, 28.68],
  山东省: [117.0, 36.65],
  河南省: [113.65, 34.76],
  湖北省: [114.3, 30.59],
  湖南省: [112.98, 28.19],
  广东省: [113.28, 23.13],
  广西壮族自治区: [108.32, 22.82],
  海南省: [110.32, 20.03],
  重庆市: [106.55, 29.56],
  四川省: [104.07, 30.57],
  贵州省: [106.71, 26.57],
  云南省: [102.71, 25.04],
  西藏自治区: [91.11, 29.97],
  陕西省: [108.95, 34.27],
  甘肃省: [103.83, 36.06],
  青海省: [101.78, 36.62],
  宁夏回族自治区: [106.28, 38.47],
  新疆维吾尔自治区: [87.62, 43.79],
  台湾省: [121.52, 25.03],
  香港特别行政区: [114.17, 22.32],
  澳门特别行政区: [113.55, 22.2],
}

/** geo 记录 → 按原文省名建索引 */
export function buildProvinceIndex(provinces: GeoProvince[]): Map<string, GeoProvince> {
  return new Map(provinces.map((item) => [item.province, item]))
}

/** 去掉行政后缀（'内蒙古自治区'→'内蒙古'，'香港特别行政区'→'香港'） */
function stripSuffix(name: string): string {
  return name
    .replace(/(维吾尔|壮族|回族)?(自治区|省|市)$/, '')
    .replace(/特别行政区$/, '')
}

/** GeoJSON 省名 → geo 数据（境外 / 无数据省份返回 undefined，即不着色不打点）。
 * 匹配顺序：DataV 全名精确 → 去后缀短名（ip2region 存短名如 '内蒙古'）→ 双向前缀。 */
export function matchProvinceData(
  geoName: string,
  index: Map<string, GeoProvince>,
): GeoProvince | undefined {
  const exact = index.get(geoName)
  if (exact) return exact
  const base = stripSuffix(geoName)
  if (!base) return undefined
  const short = index.get(base)
  if (short) return short
  for (const [key, value] of index) {
    const keyBase = stripSuffix(key)
    if (keyBase && (base.startsWith(keyBase) || key.startsWith(base))) {
      return value
    }
  }
  return undefined
}

/** map series 数据：有访问数据的省份按 count 着色（visualMap 消费 value） */
export function buildMapSeriesData(
  provinces: GeoProvince[],
  geoNames: string[],
): { name: string; value: number }[] {
  const index = buildProvinceIndex(provinces)
  const data: { name: string; value: number }[] = []
  for (const geoName of geoNames) {
    const hit = matchProvinceData(geoName, index)
    if (hit) {
      data.push({ name: geoName, value: hit.count })
    }
  }
  return data
}

/** effectScatter 打点数据：省级聚合在省会坐标；value 为访问次数（气泡大小） */
export function buildScatterData(provinces: GeoProvince[]): {
  name: string
  value: [number, number, number]
}[] {
  const data: { name: string; value: [number, number, number] }[] = []
  for (const item of provinces) {
    const center = PROVINCE_CENTERS[item.province]
    if (!center) continue
    data.push({ name: item.province, value: [center[0], center[1], item.count] })
  }
  return data
}

/** hover tooltip 参数（ECharts TopLevelFormatterParams 的宽松视图：只取用到的字段，
 * data 用 unknown 承接 OptionDataItem 全形态，运行时再窄化） */
export interface TooltipRawParams {
  name?: unknown
  data?: unknown
  seriesType?: unknown
}

function firstString(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

/** hover tooltip：`该省访问 N 次` + 城市 TOP5（无数据省份只显示省名） */
export function formatProvinceTooltip(params: unknown, provinces: GeoProvince[]): string {
  const raw = (params ?? {}) as TooltipRawParams
  const data = raw?.data
  const dataName =
    typeof data === 'object' && data !== null && 'name' in data
      ? firstString(data.name)
      : ''
  const name = dataName || firstString(raw?.name)
  const index = buildProvinceIndex(provinces)
  const hit = matchProvinceData(name, index)
  if (!hit) {
    return name ? `<b>${escapeHtml(name)}</b><br/>暂无访问数据` : ''
  }
  const lines = [`<b>${escapeHtml(hit.province)}</b>`, `该省访问 ${hit.count} 次`]
  if (hit.cities.length > 0) {
    lines.push(
      ...hit.cities.map((city, i) => `${i + 1}. ${escapeHtml(city.city)} · ${city.count}`),
    )
  }
  return lines.join('<br/>')
}

/** 输出转义（防 XSS；留言内容同口径，见 MASTER 安全约定） */
export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** ECharts 颜色集（全部经 readCssVar 从 token 读取，见 chartTheme.ts 同口径） */
export interface MapChartColors {
  green: string
  greenGlow: string
  cyan: string
  magenta: string
  amber: string
  text: string
  textDim: string
  border: string
  panel: string
  bg: string
}

export interface ChinaMapOptionInput {
  provinces: GeoProvince[]
  geoNames: string[]
  colors: MapChartColors
  /** prefers-reduced-motion：呼吸涟漪降级为静态散点（MASTER 铁律） */
  reducedMotion: boolean
}

/** 组装中国地图 option：省级 choropleth + 省会呼吸打点 */
export function buildChinaMapOption(input: ChinaMapOptionInput) {
  const { provinces, geoNames, colors, reducedMotion } = input
  const mapData = buildMapSeriesData(provinces, geoNames)
  const scatterData = buildScatterData(provinces)

  return {
    animation: !reducedMotion,
    tooltip: {
      trigger: 'item' as const,
      backgroundColor: colors.bg,
      borderColor: colors.border,
      textStyle: { color: colors.text, fontFamily: 'inherit' },
      // formatter 入参含 CallbackDataParams | CallbackDataParams[] 双形态，unknown 承接后在 formatProvinceTooltip 内窄化
      formatter: (params: unknown) => formatProvinceTooltip(params, provinces),
    },
    visualMap: {
      min: 1,
      max: Math.max(1, ...provinces.map((p) => p.count)),
      calculable: false,
      show: false,
      inRange: { color: [colors.cyan, colors.green] },
    },
    geo: {
      map: 'china',
      roam: false,
      zoom: 1.12,
      itemStyle: {
        areaColor: colors.panel,
        borderColor: colors.border,
      },
      emphasis: {
        itemStyle: { areaColor: colors.greenGlow, borderColor: colors.green },
        label: { show: false },
      },
      select: { itemStyle: { areaColor: colors.panel }, label: { show: false } },
    },
    series: [
      {
        type: 'map' as const,
        map: 'china',
        geoIndex: 0,
        name: '访问分布',
        data: mapData,
      },
      {
        // 呼吸打点：reduced-motion 时降级为普通 scatter（涟漪属特效，不受 animation 开关控制）
        ...(reducedMotion ? { type: 'scatter' as const } : { type: 'effectScatter' as const }),
        coordinateSystem: 'geo',
        name: '访问打点',
        symbolSize: (val: [number, number, number]) => Math.min(8 + val[2] * 2, 26),
        rippleEffect: { scale: 2.4, brushType: 'stroke' as const },
        itemStyle: { color: colors.magenta, shadowBlur: 8, shadowColor: colors.greenGlow },
        label: { show: false },
        data: scatterData,
        zlevel: 2,
      },
    ],
  }
}
