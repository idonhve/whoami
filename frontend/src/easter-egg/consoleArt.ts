import { cssVar } from '@/utils/cssVar'

/**
 * F11 控制台彩蛋文案与联系方式（Spec 11）：
 * 全部集中在这一个常量对象里，换文案 / 招聘联系方式只改这一处（用户故事 7）。
 *
 * 站主信息来源：GitHub 主页 github.com/idonhve（仓库 owner，site_config 的
 * github_url 同源）；邮箱 2155539871@qq.com（站主提供，仓库 site_config 暂无邮箱键）。
 */
export const EASTER_EGG_COPY = {
  /** ASCII art 站主签名（等宽字体 + 深底浅字下最佳） */
  ascii: [
    '██╗    ██╗██╗  ██╗ ██████╗  █████╗ ███╗   ███╗██╗',
    '██║    ██║██║  ██║██╔═══██╗██╔══██╗████╗ ████║██║',
    '██║ █╗ ██║███████║██║   ██║███████║██╔████╔██║██║',
    '██║███╗██║██╔══██║██║   ██║██╔══██║██║╚██╔╝██║██║',
    '╚███╔███╔╝██║  ██║╚██████╔╝██║  ██║██║ ╚═╝ ██║██║',
    ' ╚══╝╚══╝ ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝',
  ].join('\n'),
  /** "对的人"文案 */
  message: '你打开了控制台，说明你是对的人 —— 这台终端后面正在招人。',
  /** 招聘联系方式 */
  contact: {
    githubLabel: 'GitHub',
    githubUrl: 'https://github.com/idonhve',
    emailLabel: '邮箱',
    email: '2155539871@qq.com',
  },
  /** 秘技提示 */
  hint: '秘技：↑ ↑ ↓ ↓ ← → ← → B A',
} as const

/**
 * 控制台彩蛋一次性输出（应用挂载后由 initEasterEgg 调用）。
 * 只对"可见"的秘籍触发打点（easter_egg / konami）；console 输出本身无法可靠探测
 * 是否被看到，不打点避免误报（Spec 11）。
 * 颜色唯一来源仍是设计 token：getComputedStyle 取不到时回退与 :root 一致的值
 * （同 BootAnimation 粒子调色板的先例）。
 */
export function printConsoleEgg(): void {
  const neon = cssVar('--green', '#00ff9c')
  const glow = cssVar('--green-glow', 'rgba(0,255,156,.35)')
  const dim = cssVar('--text-dim', '#64788f')
  const cyan = cssVar('--cyan', '#2bd9ff')
  const { ascii, message, hint } = EASTER_EGG_COPY
  const { githubLabel, githubUrl, emailLabel, email } = EASTER_EGG_COPY.contact
  const font = 'font-family:monospace;white-space:pre;'
  const lines = [
    `%c${ascii}`,
    '',
    `%c${message}`,
    `%c${githubLabel}: ${githubUrl}`,
    `%c${emailLabel}: ${email}`,
    `%c${hint}`,
  ]
  console.log(
    lines.join('\n'),
    `color:${neon};text-shadow:0 0 8px ${glow};${font}`,
    `color:${dim};${font}`,
    `color:${cyan};${font}`,
    `color:${neon};${font}`,
    `color:${dim};${font}`,
  )
}
