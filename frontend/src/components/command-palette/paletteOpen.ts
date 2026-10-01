import { ref } from 'vue'

/**
 * 命令面板开关（Spec 10）：微全局状态。
 * 页头 `>_` 图标与全局快捷键都从这里打开；面板组件 v-if 消费。
 */

export const paletteOpen = ref(false)

export function openPalette(): void {
  paletteOpen.value = true
}

export function closePalette(): void {
  paletteOpen.value = false
}
