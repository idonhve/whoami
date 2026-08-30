import { defineStore } from 'pinia'

import { defaultSiteConfig, fetchSiteConfig, type SiteConfig } from '@/api/siteConfig'

/**
 * 站点配置 store：全站只加载一次，失败时保持默认值。
 * 消费方：开机日志域名（Spec 01）、Hero 站主名称、降级开关等。
 */
export const useSiteStore = defineStore('site', {
  state: () => ({
    config: { ...defaultSiteConfig } as SiteConfig,
    loaded: false,
  }),
  actions: {
    async load(): Promise<SiteConfig> {
      if (this.loaded) return this.config
      this.config = await fetchSiteConfig()
      this.loaded = true
      return this.config
    },
  },
})
