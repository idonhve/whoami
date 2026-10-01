<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import {
  createCommandRegistry,
  type PaletteCommand,
} from '@/components/command-palette/commands'
import { matchCommands } from '@/components/command-palette/fuzzyMatch'
import { loadHistory, pushHistory } from '@/components/command-palette/history'
import { closePalette, paletteOpen } from '@/components/command-palette/paletteOpen'
import { prefersReducedMotion } from '@/utils/motion'

/**
 * 全局命令面板（Spec 10）：Ctrl+K / Cmd+K 或页头 `>_` 图标唤起。
 * 键盘模型：Esc 关闭、↑/↓ 选择、Enter 执行；help/theme 执行后面板保留。
 */

const router = useRouter()
const commands = createCommandRegistry(router)

const input = ref('')
const inputEl = ref<HTMLInputElement | null>(null)
const activeIndex = ref(0)
/** 回显行（终端输出区）；not found 行前缀标记 */
const echoLines = ref<{ text: string; kind: 'echo' | 'error' }[]>([])

/** 当前输入命中的命令（空输入 = 全量） */
const filtered = computed<PaletteCommand[]>(() => matchCommands(input.value, commands))

/** 空输入时展示历史（仍存在于注册表中的条目） */
const historyCommands = computed<PaletteCommand[]>(() => {
  if (input.value.trim()) return []
  const byName = new Map(commands.map((cmd) => [cmd.name, cmd]))
  return loadHistory()
    .map((name) => byName.get(name))
    .filter((cmd): cmd is PaletteCommand => Boolean(cmd))
})

/** 可见行：空输入 = 历史 + 全量；有输入 = 过滤结果 */
const visible = computed<PaletteCommand[]>(() =>
  input.value.trim() ? filtered.value : [...historyCommands.value, ...commands],
)

/** 空输入且有历史时显示分区标题 */
const showHistory = computed(() => !input.value.trim() && historyCommands.value.length > 0)

function resetState(): void {
  input.value = ''
  activeIndex.value = 0
  echoLines.value = []
}

/** 执行延迟（回显可见时长）；reduced-motion 直出直隐 */
const CLOSE_DELAY_MS = 420

async function execute(command: PaletteCommand): Promise<void> {
  pushHistory(command.name)
  const echo = await command.run()
  const notFound = echo?.startsWith('command not found') ?? false
  if (echo) echoLines.value.push({ text: echo, kind: notFound ? 'error' : 'echo' })
  // help / theme / 失败回显不导航：保留面板看全量列表 / 实时换色 / 错误提示不闪没
  if (command.name === 'help' || command.name === 'theme' || notFound) {
    if (command.name === 'help') input.value = ''
    activeIndex.value = 0
    return
  }
  const delay = prefersReducedMotion() ? 0 : CLOSE_DELAY_MS
  window.setTimeout(() => closePalette(), delay)
}

function onInputKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    event.preventDefault()
    closePalette()
    return
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = Math.min(activeIndex.value + 1, visible.value.length - 1)
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = Math.max(activeIndex.value - 1, 0)
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    const target = visible.value[activeIndex.value]
    if (target) {
      void execute(target)
    } else {
      echoLines.value.push({ text: `command not found: ${input.value.trim()}`, kind: 'error' })
    }
  }
}

function onGlobalKeydown(event: KeyboardEvent): void {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    // capture 阶段拦截：页面输入框聚焦时 Ctrl+K 仍唤起（Spec 10）
    event.preventDefault()
    event.stopPropagation()
    if (paletteOpen.value) {
      closePalette()
    } else {
      paletteOpen.value = true
    }
  }
}

function onBackdropClick(event: MouseEvent): void {
  if (event.target === event.currentTarget) closePalette()
}

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown, { capture: true })
})

onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKeydown, { capture: true })
})

watch(paletteOpen, (open) => {
  if (open) {
    resetState()
    void nextTick(() => inputEl.value?.focus())
  }
})

watch(input, () => {
  activeIndex.value = 0
})

watch(activeIndex, async () => {
  await nextTick()
  const el = document.querySelector('.palette-list .is-active')
  if (el && typeof el.scrollIntoView === 'function') el.scrollIntoView({ block: 'nearest' })
})

const hint = '↑↓ select · ↵ run · esc close'
</script>

<template>
  <div
    v-if="paletteOpen"
    class="palette-backdrop"
    @click="onBackdropClick"
  >
      <div
        class="palette-panel"
        role="dialog"
        aria-modal="false"
        aria-label="命令面板"
      >
        <div class="palette-input-row">
          <span class="palette-prompt" aria-hidden="true">$</span>
          <input
            ref="inputEl"
            v-model="input"
            class="palette-input"
            type="text"
            aria-label="输入命令"
            placeholder="type a command ... (help)"
            autocomplete="off"
            spellcheck="false"
            @keydown="onInputKeydown"
          />
          <span class="palette-hint" aria-hidden="true">{{ hint }}</span>
        </div>

        <div
          v-if="echoLines.length"
          class="palette-echo"
          aria-live="polite"
        >
          <div
            v-for="(line, i) in echoLines"
            :key="i"
            :class="['palette-echo-line', line.kind]"
          >
            {{ line.text }}
          </div>
        </div>

        <ul
          class="palette-list"
          role="listbox"
          aria-label="命令列表"
        >
          <template
            v-for="(cmd, i) in visible"
            :key="`${i}-${cmd.name}`"
          >
            <li
              v-if="showHistory && i === 0"
              class="palette-section"
              aria-hidden="true"
            >
              history
            </li>
            <li
              v-if="showHistory && i === historyCommands.length"
              class="palette-section"
              aria-hidden="true"
            >
              commands
            </li>
            <li
              role="option"
              :aria-selected="i === activeIndex"
              :class="[
                'palette-item',
                {
                  'is-active': i === activeIndex,
                  'is-history': showHistory && i < historyCommands.length,
                },
              ]"
              @click="execute(cmd)"
              @mousemove="activeIndex = i"
            >
              <span class="item-name">{{ cmd.name }}</span>
              <span class="item-desc">{{ cmd.desc }}</span>
            </li>
          </template>
          <li
            v-if="input.trim() && !filtered.length"
            class="palette-empty"
          >
            command not found: {{ input.trim() }}
          </li>
        </ul>
      </div>
    </div>
</template>

<style scoped>
.palette-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9500;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 12vh;
  background: color-mix(in srgb, var(--bg) 72%, transparent);
  backdrop-filter: blur(4px);
}

.palette-panel {
  width: min(560px, calc(100vw - 32px));
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  border: 2px solid var(--border-bright);
  background: var(--bg-panel);
  box-shadow:
    0 0 28px var(--green-glow),
    inset 0 0 18px color-mix(in srgb, var(--green) 4%, transparent);
  animation: palette-in 0.16s steps(2) both;
}

@keyframes palette-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.palette-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
}

.palette-prompt {
  color: var(--magenta);
  font-family: var(--font-term);
  font-size: 24px;
}

.palette-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text);
  font-family: var(--font-term);
  font-size: 24px;
  letter-spacing: 1px;
  caret-color: var(--green);
}

.palette-input::placeholder {
  color: var(--text-dim);
  opacity: 0.7;
}

.palette-hint {
  color: var(--text-dim);
  font-size: 11px;
  font-family: var(--font-mono);
  white-space: nowrap;
}

.palette-echo {
  padding: 8px 14px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-raised);
}

.palette-echo-line {
  font-family: var(--font-term);
  font-size: 19px;
  letter-spacing: 0.5px;
  color: var(--text-dim);
  white-space: pre-wrap;
  word-break: break-all;
}

.palette-echo-line:last-child {
  color: var(--green);
}

.palette-echo-line.error {
  color: var(--error);
}

.palette-list {
  margin: 0;
  padding: 6px 0;
  list-style: none;
  overflow-y: auto;
}

.palette-section {
  padding: 6px 14px 2px;
  color: var(--text-dim);
  font-size: 11px;
  font-family: var(--font-pixel);
  letter-spacing: 1px;
}

.palette-item {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 8px 14px;
  cursor: pointer;
  border-left: 2px solid transparent;
}

.palette-item.is-history .item-name::after {
  content: ' *';
  color: var(--text-dim);
}

.palette-item.is-active {
  background: var(--green-soft);
  border-left-color: var(--green);
}

.item-name {
  min-width: 96px;
  font-family: var(--font-term);
  font-size: 20px;
  color: var(--text);
}

.palette-item.is-active .item-name {
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
}

.item-desc {
  color: var(--text-dim);
  font-size: 13px;
}

.palette-empty {
  padding: 10px 14px;
  color: var(--error);
  font-family: var(--font-term);
  font-size: 19px;
}

@media (max-width: 720px) {
  .palette-backdrop {
    padding-top: 6vh;
    align-items: stretch;
  }

  .palette-panel {
    max-height: 82vh;
  }

  .palette-hint {
    display: none;
  }
}
</style>
