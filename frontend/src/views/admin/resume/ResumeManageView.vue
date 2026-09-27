<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  fetchAdminResumes,
  restoreResume,
  uploadResume,
  type ResumeVersion,
} from '@/api/resume'
import { formatSize, validateResumeFile } from './uploadRules'

/**
 * 简历管理页（Spec 07）：上传新版本（multipart，仅 pdf ≤ 20MB，前端预校验）
 * + 版本列表（versionNo/displayName/sizeBytes/isCurrent/uploadedAt，倒序，历史保留最近 3 个）
 * + 回滚（PUT /admin/api/resumes/{id}/restore，确认弹窗防误触）。
 */

const versions = ref<ResumeVersion[]>([])
const loading = ref(true)
const errorMsg = ref('')
const notice = ref('')

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const uploading = ref(false)

const confirming = ref<ResumeVersion | null>(null)
const restoring = ref(false)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    versions.value = await fetchAdminResumes()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function pickFile() {
  fileInput.value?.click()
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  errorMsg.value = ''
  notice.value = ''
  if (!file) {
    selectedFile.value = null
    return
  }
  const invalid = validateResumeFile(file)
  if (invalid) {
    selectedFile.value = null
    errorMsg.value = `[reject] ${invalid}：${file.name}（${formatSize(file.size)}）`
  } else {
    selectedFile.value = file
  }
  // 允许重复选择同一文件再次触发 change
  input.value = ''
}

async function upload() {
  const file = selectedFile.value
  if (!file || uploading.value) return
  uploading.value = true
  errorMsg.value = ''
  notice.value = ''
  try {
    const result = await uploadResume(file)
    selectedFile.value = null
    notice.value =
      result.evictedVersionNos.length > 0
        ? `[ok] v${result.versionNo} 已上传并置为当前版本，历史版本 ${result.evictedVersionNos.map((n) => `v${n}`).join('/')} 已淘汰（仅保留最近 3 个）`
        : `[ok] v${result.versionNo} 已上传并置为当前版本，前台按钮即时生效`
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '上传失败'
  } finally {
    uploading.value = false
  }
}

function askRestore(item: ResumeVersion) {
  confirming.value = item
  errorMsg.value = ''
  notice.value = ''
}

function cancelRestore() {
  confirming.value = null
}

async function confirmRestore() {
  const target = confirming.value
  if (!target || restoring.value) return
  restoring.value = true
  errorMsg.value = ''
  notice.value = ''
  try {
    await restoreResume(target.id)
    confirming.value = null
    notice.value = `[ok] 已回滚到 v${target.versionNo}（${target.displayName}），前台下载即时切换`
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '回滚失败'
  } finally {
    restoring.value = false
  }
}

function fmtTime(iso: string | null) {
  return iso ? iso.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="resume-admin">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        RESUME / VERSIONS
      </h1>
      <p class="sub">$ resume --upload --keep-last 3 · 上传即替换当前版本，历史可回滚</p>
    </header>

    <div class="upload-bar">
      <input
        ref="fileInput"
        type="file"
        accept="application/pdf,.pdf"
        class="file-input"
        aria-label="选择简历 PDF"
        @change="onFileChange"
      />
      <button class="op-btn pick" :disabled="uploading" @click="pickFile">&gt; 选择 PDF</button>
      <span v-if="selectedFile" class="picked">
        {{ selectedFile.name }}（{{ formatSize(selectedFile.size) }}）
      </span>
      <button class="sync-btn" :disabled="!selectedFile || uploading" @click="upload">
        {{ uploading ? 'uploading ...' : '> 上传新版本' }}
      </button>
    </div>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="errorMsg" class="error" role="alert">{{ errorMsg }}</p>

    <p v-if="loading" class="loading">loading ...</p>

    <table v-else class="term-table">
      <thead>
        <tr>
          <th scope="col">版本</th>
          <th scope="col">显示文件名</th>
          <th scope="col">大小</th>
          <th scope="col">状态</th>
          <th scope="col">上传时间</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="versions.length === 0">
          <td colspan="6" class="empty-cell">尚未上传简历 —— 前台下载按钮保持隐藏</td>
        </tr>
        <tr v-for="item in versions" :key="item.id" :class="{ 'row-current': item.isCurrent }">
          <td class="ver">v{{ item.versionNo }}</td>
          <td class="name">{{ item.displayName }}</td>
          <td class="stat">{{ formatSize(item.sizeBytes) }}</td>
          <td class="badges">
            <span v-if="item.isCurrent" class="badge current">CURRENT</span>
            <span v-else class="badge none">-</span>
          </td>
          <td class="time">{{ fmtTime(item.uploadedAt) }}</td>
          <td class="ops">
            <button
              v-if="!item.isCurrent"
              class="op-btn"
              :disabled="restoring"
              @click="askRestore(item)"
            >
              回滚
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div
      v-if="confirming"
      class="confirm-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="确认回滚"
      @click.self="cancelRestore"
    >
      <div class="confirm-box hud-frame">
        <p class="confirm-cmd">$ resume --restore v{{ confirming.versionNo }}</p>
        <p class="confirm-text">
          将 <strong>{{ confirming.displayName }}</strong> 置为当前版本？
          前台下载会立即切换为该文件，其它历史版本不受影响。
        </p>
        <div class="confirm-ops">
          <button class="op-btn danger" :disabled="restoring" @click="confirmRestore">
            {{ restoring ? '...' : '确认回滚' }}
          </button>
          <button class="op-btn" :disabled="restoring" @click="cancelRestore">取消</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.resume-admin {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-family: var(--font-pixel);
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 2px;
  color: var(--green);
  text-shadow: 0 0 10px var(--green-glow);
}

.led {
  width: 10px;
  height: 10px;
  background: var(--green);
  box-shadow: 0 0 10px var(--green-glow);
  animation: led-pulse 2.4s ease-in-out infinite;
}

.sub {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 20px;
  letter-spacing: 0.5px;
}

.upload-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

.picked {
  color: var(--cyan);
  font-size: 13px;
  word-break: break-all;
}

.sync-btn {
  background: transparent;
  border: 2px solid var(--green);
  color: var(--green);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 10px 20px;
  min-height: 44px;
  cursor: pointer;
  text-shadow: 0 0 6px var(--green-glow);
  box-shadow: 0 0 10px var(--green-soft);
  transition:
    background 0.2s,
    box-shadow 0.2s,
    transform 0.15s;
}

.sync-btn:hover:not(:disabled) {
  background: var(--green-soft);
  box-shadow: 0 0 18px var(--green-glow);
  transform: translateY(-1px);
}

.sync-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.notice {
  margin: 0;
  color: var(--green);
  font-size: 13px;
}

.error {
  margin: 0;
  color: var(--error);
  font-size: 13px;
  word-break: break-all;
}

.loading {
  margin: 0;
  color: var(--text-dim);
}

.row-current {
  background: var(--green-soft);
}

.ver {
  font-family: var(--font-term);
  font-size: 17px;
  color: var(--cyan);
  white-space: nowrap;
}

.name {
  word-break: break-all;
}

.stat {
  white-space: nowrap;
}

.badges {
  white-space: nowrap;
}

.badge {
  display: inline-block;
  font-family: var(--font-pixel);
  font-size: 8px;
  letter-spacing: 1px;
  padding: 3px 5px;
  border: 1px solid var(--border-bright);
  color: var(--text-dim);
}

.badge.current {
  color: var(--green);
  border-color: var(--green);
  text-shadow: 0 0 6px var(--green-glow);
}

.badge.none {
  border-color: var(--border);
}

.time {
  color: var(--text-dim);
  font-size: 12px;
  white-space: nowrap;
}

.ops {
  white-space: nowrap;
}

.op-btn {
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 4px 12px;
  margin-right: 6px;
  min-height: 32px;
  transition:
    border-color 0.2s,
    color 0.2s,
    box-shadow 0.2s;
}

.op-btn:hover:not(:disabled) {
  border-color: var(--green);
  color: var(--green);
  box-shadow: 0 0 10px var(--green-soft);
}

.op-btn.pick {
  padding: 10px 20px;
  min-height: 44px;
  font-size: 13px;
}

.op-btn.danger {
  border-color: var(--amber);
  color: var(--amber);
}

.op-btn.danger:hover:not(:disabled) {
  border-color: var(--amber);
  color: var(--amber);
  box-shadow: 0 0 10px var(--green-soft);
}

.op-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.empty-cell {
  color: var(--text-dim);
  text-align: center;
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: color-mix(in srgb, var(--bg) 82%, transparent);
}

.confirm-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: min(480px, 100%);
  padding: 24px;
  background: var(--bg-panel);
  border: 1px solid var(--border-bright);
}

.confirm-cmd {
  margin: 0;
  font-family: var(--font-term);
  font-size: 20px;
  color: var(--amber);
}

.confirm-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text);
}

.confirm-text strong {
  color: var(--green);
  word-break: break-all;
}

.confirm-ops {
  display: flex;
  gap: 8px;
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }

  .sync-btn:hover:not(:disabled) {
    transform: none;
  }
}
</style>
