import type { Router } from 'vue-router'

import { RESUME_DOWNLOAD_URL, getLatest } from '@/api/resume'
import { fetchSiteConfig } from '@/api/siteConfig'
import { trackEvent } from '@/tracker'
import { cycleTheme, type ThemePreset } from '@/components/command-palette/theme'

/**
 * 命令注册表（Spec 10）：集中本模块维护，不开放外部注册。
 * 命令名即用户输入；执行含回显文案（动作词表集中常量）。
 */

/** 回显动作词表：`> <动作> <目标> ...`（Spec 10 Further Notes） */
export const ACTION_VERBS = {
  navigating: 'navigating to',
  downloading: 'downloading',
  opening: 'opening',
  switching: 'switching theme to',
  listing: 'listing',
} as const

export interface PaletteCommand {
  name: string
  desc: string
  /** 返回回显行（如 "> navigating to works ..."）；返回 null 表示已自行处理回显 */
  run: () => Promise<string | null>
}

function downloadByAnchor(): void {
  const a = document.createElement('a')
  a.href = RESUME_DOWNLOAD_URL
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

/** message 命令：导航 /about 后聚焦留言板首个输入框（锚点约定 #message-board） */
async function navigateAndFocusMessageBoard(router: Router): Promise<void> {
  await router.push('/about')
  window.setTimeout(() => {
    const board = document.querySelector('#message-board')
    const target = board?.querySelector('input, textarea')
    if (target instanceof HTMLElement) target.focus()
  }, 120)
}

/**
 * 创建命令注册表（命令行为需要 router 实例）。
 * 埋点：执行命令统一上报 cmd_palette_use（detail.command）。
 */
export function createCommandRegistry(router: Router): PaletteCommand[] {
  const navigate = async (path: string, name: string): Promise<string> => {
    trackEvent('cmd_palette_use', { command: name })
    await router.push(path)
    return `> ${ACTION_VERBS.navigating} ${name} ...`
  }

  return [
    { name: 'home', desc: '回到首页', run: () => navigate('/', 'home') },
    { name: 'works', desc: '作品展示', run: () => navigate('/works', 'works') },
    { name: 'tech', desc: '技术栈', run: () => navigate('/tech', 'tech') },
    {
      name: 'experience',
      desc: '工作经历',
      run: () => navigate('/experience', 'experience'),
    },
    { name: 'awards', desc: '证书照片墙', run: () => navigate('/awards', 'awards') },
    {
      name: 'resume',
      desc: '下载简历',
      run: async () => {
        trackEvent('cmd_palette_use', { command: 'resume' })
        const latest = await getLatest()
        if (!latest.exists) {
          return `command not found: resume（站主还未上传简历）`
        }
        downloadByAnchor()
        return `> ${ACTION_VERBS.downloading} ${latest.displayName ?? 'resume'} ...`
      },
    },
    {
      name: 'message',
      desc: '去留言',
      run: async () => {
        trackEvent('cmd_palette_use', { command: 'message' })
        await navigateAndFocusMessageBoard(router)
        return `> ${ACTION_VERBS.navigating} message-board ...`
      },
    },
    {
      name: 'github',
      desc: '打开站主 GitHub',
      run: async () => {
        trackEvent('cmd_palette_use', { command: 'github' })
        const config = await fetchSiteConfig()
        const url = config.githubUrl
        if (!url) {
          return `command not found: github（未配置 githubUrl）`
        }
        trackEvent('github_outbound', { source: 'palette' })
        window.open(url, '_blank', 'noopener,noreferrer')
        return `> ${ACTION_VERBS.opening} github ...`
      },
    },
    {
      name: 'theme',
      desc: '切换强调色',
      run: async () => {
        trackEvent('cmd_palette_use', { command: 'theme' })
        const current = (document.documentElement.getAttribute('data-cmd-theme') ??
          'green') as ThemePreset
        const next = cycleTheme(current)
        return `> ${ACTION_VERBS.switching} ${next} ...`
      },
    },
    {
      name: 'help',
      desc: '列出全部命令',
      run: async () => {
        trackEvent('cmd_palette_use', { command: 'help' })
        return `> ${ACTION_VERBS.listing} commands ...`
      },
    },
  ]
}
