/**
 * 首页 Hero 文案与入口命令（Spec 01）。
 * 一句话定位与命令文案素材最终来自 docs/content/（素材窗口 B），
 * 素材未到位前使用占位文案，不阻塞验收。
 */

/** 打字机轮播的一句话定位（占位文案） */
export const HERO_TAGLINES = [
  '全栈工程师，把想法敲成产品。',
  '终端美学 / 像素信仰 / 赛博霓虹。',
  'Java × Vue × 一点点强迫症。',
] as const

export interface HeroCommand {
  to: string
  label: string
  desc: string
}

/** 入口命令：从 Hero 直达各核心区块 */
export const HERO_COMMANDS: HeroCommand[] = [
  { to: '/works', label: 'cd /works', desc: '精选作品' },
  { to: '/tech', label: 'cd /tech', desc: '技术栈' },
  { to: '/experience', label: 'cd /experience', desc: '工作经历' },
  { to: '/awards', label: 'cd /awards', desc: '证书墙' },
  { to: '/about', label: 'cd /about', desc: '关于我' },
]
