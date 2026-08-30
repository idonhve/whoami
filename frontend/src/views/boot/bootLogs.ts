/**
 * 开机动画伪日志文案（Spec 01：≥ 5 行，域名从站点配置注入）。
 * 纯函数便于契约测试：改 site_config.domain 后日志文案随之变化。
 */

export interface BootLogLine {
  text: string
  /** ok=主绿 dim=次要 accent=高亮收尾 */
  kind: 'ok' | 'dim' | 'accent'
}

export function buildBootLogs(domain: string): BootLogLine[] {
  return [
    { text: `> initializing ${domain} ...`, kind: 'ok' },
    { text: '> loading portfolio ...', kind: 'ok' },
    { text: '> mounting hero interface ........ [ OK ]', kind: 'dim' },
    { text: '> calibrating neon grid .......... [ OK ]', kind: 'dim' },
    { text: '> verifying visitor session ...... [ OK ]', kind: 'dim' },
    { text: '> welcome, guest. access granted.', kind: 'accent' },
  ]
}
