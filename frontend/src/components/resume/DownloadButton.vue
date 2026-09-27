<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { RESUME_DOWNLOAD_URL, getLatest, type ResumeLatest } from '@/api/resume'

/**
 * 简历下载按钮（Spec 07，全站唯一实现）。
 * - 终端风命令样式（`> download <displayName>`）+ hover 光效（扫描亮带 + 霓虹光晕）
 * - exists=false / 接口异常时整体隐藏且不报错（先调 latest 再决定渲染）
 * - 点击即浏览器导航下载，无跳转页、无二次确认；埋点由服务端直写
 *
 * TODO(F5/about): about 页（Spec 05 前端）创建后，在其预留的下载按钮 slot
 * 直接复用本组件，不要另写第二份实现。
 */

const latest = ref<ResumeLatest | null>(null)

const fileName = computed(() => latest.value?.displayName?.trim() || 'resume.pdf')

const updatedLabel = computed(() => {
  const iso = latest.value?.updatedAt
  return iso ? iso.replace('T', ' ').slice(0, 10) : ''
})

onMounted(async () => {
  try {
    latest.value = await getLatest()
  } catch {
    // 接口异常按无简历处理：按钮隐藏且不报错
    latest.value = { exists: false, displayName: null, updatedAt: null }
  }
})
</script>

<template>
  <a
    v-if="latest?.exists"
    class="resume-download"
    :href="RESUME_DOWNLOAD_URL"
    :download="fileName"
    role="button"
    :aria-label="`下载简历 ${fileName}`"
  >
    <span class="beam" aria-hidden="true"></span>
    <svg class="icon" width="14" height="14" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
      <path d="M7 1h2v7h2.5L8 11.5 4.5 8H7zM2 13h12v2H2z" />
    </svg>
    <span class="cmd">
      <span class="prompt" aria-hidden="true">&gt;</span>
      <span class="cmd-text">download {{ fileName }}</span>
      <span class="cursor" aria-hidden="true">▌</span>
    </span>
    <span v-if="updatedLabel" class="meta"># updated {{ updatedLabel }}</span>
  </a>
</template>

<style scoped>
.resume-download {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 10px 20px;
  overflow: hidden;
  border: 2px solid var(--green);
  background: var(--bg-panel);
  color: var(--green);
  font-family: var(--font-term);
  font-size: 20px;
  letter-spacing: 0.5px;
  text-decoration: none;
  text-shadow: 0 0 6px var(--green-glow);
  box-shadow: 0 0 10px var(--green-soft);
  transition:
    background 0.2s,
    box-shadow 0.2s,
    transform 0.15s;
}

/* hover 光效：底色点亮 + 光晕增强 + 上浮 + 扫描亮带扫过（只动 transform/opacity） */
.resume-download:hover,
.resume-download:focus-visible {
  background: var(--green-soft);
  box-shadow:
    0 0 18px var(--green-glow),
    inset 0 0 12px var(--green-soft);
  transform: translateY(-2px);
}

.resume-download:focus-visible {
  outline: 2px solid var(--cyan);
  outline-offset: 2px;
}

.beam {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -30%;
  width: 24%;
  background: linear-gradient(90deg, transparent, var(--green-glow), transparent);
  opacity: 0;
  transform: translateX(0);
  pointer-events: none;
}

.resume-download:hover .beam,
.resume-download:focus-visible .beam {
  opacity: 1;
  animation: resume-beam-sweep 0.7s steps(8) 1;
}

@keyframes resume-beam-sweep {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(520%);
  }
}

.icon {
  flex: none;
  filter: drop-shadow(0 0 4px var(--green-glow));
}

.cmd {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  white-space: nowrap;
}

.prompt {
  color: var(--magenta);
  text-shadow: 0 0 6px var(--green-glow);
}

.cursor {
  color: var(--green);
  animation: cursor-blink 1s steps(1) infinite;
}

.meta {
  color: var(--text-dim);
  font-family: var(--font-mono);
  font-size: 12px;
  text-shadow: none;
  white-space: nowrap;
}

@media (prefers-reduced-motion: reduce) {
  .resume-download,
  .resume-download:hover,
  .resume-download:focus-visible {
    transition: none;
    transform: none;
  }

  .beam {
    display: none;
  }

  .cursor {
    animation: none;
  }
}
</style>
