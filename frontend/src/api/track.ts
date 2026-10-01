import { trackEvent } from '@/tracker'

/**
 * 埋点便捷封装（Spec 05 交付 SDK 后由其统一接管上报）。
 * 三个导出函数为 F3/F4/F11 既有调用点，签名与行为保持不变：
 * fire-and-forget、sendBeacon 优先、失败静默不影响外跳/彩蛋。
 */

/** 卡片外跳 GitHub 计入埋点（不 await、不抛错） */
export function trackGithubOutbound(repoName: string): void {
  trackEvent('github_outbound', repoName)
}

/** 页头/页脚 GitHub 图标外跳计入埋点（Spec 03，detail 带来源） */
export function trackGithubIconClick(source: 'header' | 'footer'): void {
  trackEvent('github_outbound', { source })
}

/** 彩蛋秘籍触发计入埋点（Spec 11：console 输出无法可靠探测是否被看到，不打点避免误报） */
export function trackEasterEgg(type: 'konami'): void {
  trackEvent('easter_egg', { type })
}
