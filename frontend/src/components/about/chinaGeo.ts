/**
 * 中国地图 GeoJSON（阿里 DataV.GeoAtlas 标准版图 100000_full，版本锁定入库不依赖 CDN）：
 * 34 个省级行政区 + 南海诸岛九段线 feature（adcode=100000_JD，properties.name 为空）。
 * 本模块负责解析与省名提取（纯函数，可单测）。
 */

export interface GeoFeatureProperties {
  name?: string
  adcode?: string | number
  level?: string
}

export interface GeoFeature {
  type: 'Feature'
  properties: GeoFeatureProperties
  geometry: unknown
}

export interface ChinaGeoJson {
  type: 'FeatureCollection'
  features: GeoFeature[]
}

/** 解析 ?raw 导入的 GeoJSON 字符串；结构非法时抛错（入库文件损坏应在开发期暴露） */
export function parseChinaGeo(raw: string): ChinaGeoJson {
  const parsed = JSON.parse(raw) as ChinaGeoJson
  if (parsed?.type !== 'FeatureCollection' || !Array.isArray(parsed.features)) {
    throw new Error('china.json 不是合法的 GeoJSON FeatureCollection')
  }
  return parsed
}

/** 提取省级行政区名称（DataV properties.name 与 ip2region 省名同口径，如"广东省"） */
export function extractGeoProvinceNames(geo: ChinaGeoJson): string[] {
  return geo.features
    .map((f) => f.properties?.name)
    .filter((name): name is string => typeof name === 'string' && name.length > 0)
}
