/**
 * 降级模式判定（Spec 01）：
 * - navigator.hardwareConcurrency ≤ 4 或视口宽 < 768px → 降级（静态渐变背景 + 轻量 CSS 动效）
 * - site_config.degrade_force_full = true 时强制满血版（站主预览 3D 用）
 */

export const DEGRADE_VIEWPORT_WIDTH = 768
export const DEGRADE_MAX_CORES = 4

export function isLowPowerDevice(): boolean {
  const cores = navigator.hardwareConcurrency ?? 8
  return cores <= DEGRADE_MAX_CORES || window.innerWidth < DEGRADE_VIEWPORT_WIDTH
}

/** forceFull 来自站点配置；为 true 时无论设备如何都渲染满血版 */
export function resolveDegraded(forceFull: boolean): boolean {
  if (forceFull) return false
  return isLowPowerDevice()
}
