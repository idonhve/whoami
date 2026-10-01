import { http } from '@/api/http'

/**
 * 站点配置（GET /api/site-config，契约见 docs/spec/06-admin-cms.md）。
 * 公开白名单键：domain / owner_name / github_url / degrade_force_full（JSON 为 camelCase）。
 * 值在后端统一字符串存储，消费方负责转型。
 */
export interface SiteConfig {
  domain: string
  ownerName: string
  githubUrl: string
  degradeForceFull: boolean
}

/** 接口不可用时的降级默认值（域名占位符本地为 localhost） */
export const defaultSiteConfig: SiteConfig = {
  domain: 'localhost',
  ownerName: '站主',
  githubUrl: '',
  degradeForceFull: false,
}

function asString(value: unknown, fallback: string): string {
  return typeof value === 'string' && value.length > 0 ? value : fallback
}

function asBoolean(value: unknown, fallback: boolean): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return value === 'true'
  return fallback
}

/**
 * 拉取站点配置。任何失败（网络 / 非 JSON / 业务码非 0）都回退到默认值，
 * 保证开机日志与 Hero 在接口不可用时依然真实可用。
 */
export async function fetchSiteConfig(): Promise<SiteConfig> {
  try {
    const raw = await http.get<Record<string, unknown>>('/api/site-config')
    return {
      domain: asString(raw?.domain, defaultSiteConfig.domain),
      ownerName: asString(raw?.ownerName, defaultSiteConfig.ownerName),
      githubUrl: asString(raw?.githubUrl, defaultSiteConfig.githubUrl),
      degradeForceFull: asBoolean(raw?.degradeForceFull, defaultSiteConfig.degradeForceFull),
    }
  } catch {
    return { ...defaultSiteConfig }
  }
}
