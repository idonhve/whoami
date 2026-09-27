<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'

import GithubIcon from '@/components/shared/GithubIcon.vue'
import { openPalette } from '@/components/command-palette/paletteOpen'

/**
 * 全局前台页头：命令式导航（导航即命令）。
 * 后续模块挂载点：Spec 10 追加 `>_` 命令面板图标（右侧 .header-side）。
 */

const NAV_COMMANDS = [
  { to: '/', label: 'cd ~', aria: '首页' },
  { to: '/works', label: 'cd /works', aria: '作品' },
  { to: '/tech', label: 'cd /tech', aria: '技术栈' },
  { to: '/experience', label: 'cd /experience', aria: '经历' },
  { to: '/awards', label: 'cd /awards', aria: '证书' },
  { to: '/about', label: 'cd /about', aria: '关于' },
] as const

const route = useRoute()

function isActive(to: string): boolean {
  if (to === '/') return route.path === '/'
  return route.path.startsWith(to)
}
</script>

<template>
  <header class="app-header">
    <RouterLink to="/" class="header-logo" aria-label="whoami 首页">whoami</RouterLink>

    <nav class="header-nav" aria-label="主导航">
      <RouterLink
        v-for="cmd in NAV_COMMANDS"
        :key="cmd.to"
        :to="cmd.to"
        class="header-cmd"
        :class="{ 'is-active': isActive(cmd.to) }"
        :aria-label="cmd.aria"
      >
        <span class="cmd-dollar" aria-hidden="true">$</span>{{ cmd.label }}
      </RouterLink>
    </nav>

    <!-- Spec 10 命令面板图标：>_ 唤起（键盘党用 Ctrl+K） -->
    <div class="header-side">
      <button
        type="button"
        class="palette-trigger"
        aria-label="命令面板（Ctrl+K）"
        title="命令面板 Ctrl+K"
        @click="openPalette()"
      >
        <svg
          viewBox="0 0 24 24"
          width="18"
          height="18"
          aria-hidden="true"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="square"
        >
          <path d="M4 6l6 6-6 6" />
          <path d="M12 18h8" />
        </svg>
      </button>
      <GithubIcon source="header" />
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9000;
  display: flex;
  align-items: center;
  gap: 24px;
  min-height: 56px;
  padding: 0 24px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg) 82%, transparent);
  backdrop-filter: blur(6px);
}

.header-logo {
  font-family: var(--font-pixel);
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
  padding: 12px 0;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
}

.header-nav::-webkit-scrollbar {
  display: none;
}

.header-cmd {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 8px 12px;
  font-family: var(--font-term);
  font-size: 17px;
  letter-spacing: 0.5px;
  color: var(--text-dim);
  white-space: nowrap;
  border: 1px solid transparent;
  transition:
    color 0.2s,
    border-color 0.2s,
    background 0.2s;
}

.cmd-dollar {
  color: var(--magenta);
}

.header-cmd:hover {
  color: var(--text);
}

.header-cmd.is-active {
  color: var(--green);
  border-color: var(--border-bright);
  background: var(--green-soft);
  text-shadow: 0 0 8px var(--green-glow);
}

.header-side {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.palette-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  color: var(--text-dim);
  background: transparent;
  border: 1px solid var(--border);
  cursor: pointer;
  transition:
    color 0.2s,
    border-color 0.2s,
    box-shadow 0.2s;
}

.palette-trigger:hover {
  color: var(--green);
  border-color: var(--border-bright);
  box-shadow: 0 0 12px var(--green-glow);
}

@media (max-width: 720px) {
  .app-header {
    gap: 12px;
    padding: 0 12px;
  }

  .header-cmd {
    font-size: 15px;
    padding: 8px 8px;
  }
}
</style>
