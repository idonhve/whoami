<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  CERT_MAX_SIZE_BYTES,
  createCertificate,
  deleteCertificate,
  fetchCertificates,
  isPdfCertificate,
  pdfPreviewUrl,
  updateCertificate,
  validateCertificateFile,
  type Certificate,
} from '@/api/certificate'

/**
 * 证书管理页（Spec 08）：上传（服务端自动生成缩略图与压缩原图）
 * + 行内编辑名称/获取时间/排序 + 删除（确认弹窗）。
 * 上传前预校验类型与大小，后端另有魔数校验兜底；改完前台刷新即见。
 */
const list = ref<Certificate[]>([])
const loading = ref(true)
const errorMsg = ref('')
const notice = ref('')

/** 单张上限（MB），展示与校验共用同一来源 */
const maxSizeMb = CERT_MAX_SIZE_BYTES / (1024 * 1024)

// ---- 上传表单 ----
const uploadFile = ref<File | null>(null)
const uploadName = ref('')
const uploadDate = ref('')
const uploading = ref(false)
/** 重置原生 file input 选中态用的 key */
const fileInputKey = ref(0)

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  uploadFile.value = input.files && input.files.length > 0 ? input.files[0]! : null
}

function validateUpload(): string | null {
  if (!uploadFile.value) return '请选择证书图片或 PDF 文件'
  const fileProblem = validateCertificateFile(uploadFile.value)
  if (fileProblem) return fileProblem
  if (!uploadName.value.trim()) return '证书名称不能为空'
  if (uploadName.value.trim().length > 100) return '证书名称不能超过 100 字符'
  if (!uploadDate.value) return '请选择获取时间'
  return null
}

async function submitUpload() {
  if (uploading.value) return
  const problem = validateUpload()
  if (problem || !uploadFile.value) {
    errorMsg.value = problem ?? '请选择证书图片或 PDF 文件'
    return
  }
  uploading.value = true
  errorMsg.value = ''
  notice.value = ''
  try {
    await createCertificate(uploadFile.value, uploadName.value.trim(), uploadDate.value)
    notice.value = `[ok] 已上传「${uploadName.value.trim()}」，前台刷新即见`
    uploadFile.value = null
    uploadName.value = ''
    uploadDate.value = ''
    fileInputKey.value += 1
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '上传失败'
  } finally {
    uploading.value = false
  }
}

// ---- 行内编辑 ----
const editingId = ref<number | null>(null)
const editName = ref('')
const editDate = ref('')
const editSort = ref(0)
const saving = ref(false)

function startEdit(item: Certificate) {
  editingId.value = item.id
  editName.value = item.name
  editDate.value = item.obtainedAt
  editSort.value = item.sortOrder
  errorMsg.value = ''
  notice.value = ''
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(item: Certificate) {
  if (saving.value) return
  const name = editName.value.trim()
  if (!name) {
    errorMsg.value = '证书名称不能为空'
    return
  }
  if (name.length > 100) {
    errorMsg.value = '证书名称不能超过 100 字符'
    return
  }
  if (!editDate.value) {
    errorMsg.value = '请选择获取时间'
    return
  }
  saving.value = true
  errorMsg.value = ''
  try {
    await updateCertificate(item.id, {
      name,
      obtainedAt: editDate.value,
      sortOrder: Number(editSort.value) || 0,
    })
    notice.value = `[ok] 「${name}」已保存，前台刷新即见`
    editingId.value = null
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove(item: Certificate) {
  if (saving.value || uploading.value) return
  const confirmed = window.confirm(`确认删除证书「${item.name}」？关联图片文件将一并删除`)
  if (!confirmed) return
  saving.value = true
  errorMsg.value = ''
  try {
    await deleteCertificate(item.id)
    notice.value = `[ok] 已删除「${item.name}」，前台刷新即见`
    if (editingId.value === item.id) editingId.value = null
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '删除失败'
  } finally {
    saving.value = false
  }
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    list.value = await fetchCertificates()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="awards-admin">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        CERTIFICATES / AWARDS
      </h1>
      <p class="sub">$ certificate --upload --auto-thumb · 缩略图与压缩原图服务端生成</p>
    </header>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>

    <form class="form" @submit.prevent="submitUpload">
      <div class="form-grid">
        <label class="field span-2">
          <span>证书文件 *（jpg / jpeg / png / webp / pdf，单张 ≤ {{ maxSizeMb }}MB）</span>
          <input
            :key="fileInputKey"
            class="file-input"
            type="file"
            accept="image/jpeg,image/png,image/webp,application/pdf,.pdf"
            :disabled="uploading"
            @change="onFileChange"
          />
        </label>
        <label class="field">
          <span>证书名称 *（≤ 100 字符）</span>
          <input
            v-model="uploadName"
            class="name-input"
            type="text"
            maxlength="100"
            placeholder="AWS Solutions Architect"
            spellcheck="false"
            :disabled="uploading"
          />
        </label>
        <label class="field">
          <span>获取时间 *</span>
          <input
            v-model="uploadDate"
            class="date-input"
            type="date"
            :disabled="uploading"
          />
        </label>
      </div>

      <div class="actions">
        <button class="neon-btn" type="submit" :disabled="uploading">
          {{ uploading ? 'UPLOADING…' : '+ UPLOAD' }}
        </button>
      </div>
    </form>

    <p v-if="loading" class="loading">loading ...</p>

    <table v-else class="term-table">
      <thead>
        <tr>
          <th scope="col">缩略图</th>
          <th scope="col">ID</th>
          <th scope="col">名称</th>
          <th scope="col">获取时间</th>
          <th scope="col">排序</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in list" :key="item.id">
          <td class="thumb-cell">
            <iframe
              v-if="isPdfCertificate(item)"
              class="thumb-img pdf-thumb"
              :src="pdfPreviewUrl(item.thumbUrl)"
              :title="`PDF 预览：${item.name}`"
              loading="lazy"
              tabindex="-1"
            />
            <img v-else class="thumb-img" :src="item.thumbUrl" :alt="`缩略图：${item.name}`" loading="lazy" />
          </td>
          <td class="id">{{ item.id }}</td>
          <td class="name-cell">
            <template v-if="editingId === item.id">
              <input
                v-model="editName"
                class="edit-input name-edit"
                maxlength="100"
                spellcheck="false"
                :disabled="saving"
                @keyup.enter="saveEdit(item)"
                @keyup.esc="cancelEdit"
              />
            </template>
            <template v-else>{{ item.name }}</template>
          </td>
          <td class="time-cell">
            <template v-if="editingId === item.id">
              <input v-model="editDate" class="edit-input date-edit" type="date" :disabled="saving" />
            </template>
            <template v-else>{{ item.obtainedAt }}</template>
          </td>
          <td class="sort-cell">
            <template v-if="editingId === item.id">
              <input
                v-model.number="editSort"
                class="edit-input sort-edit"
                type="number"
                step="1"
                :disabled="saving"
              />
            </template>
            <template v-else>{{ item.sortOrder }}</template>
          </td>
          <td class="ops">
            <template v-if="editingId === item.id">
              <button class="op-btn save" :disabled="saving" @click="saveEdit(item)">
                {{ saving ? '...' : '保存' }}
              </button>
              <button class="op-btn" :disabled="saving" @click="cancelEdit">取消</button>
            </template>
            <template v-else>
              <button class="op-btn edit" :disabled="saving || uploading" @click="startEdit(item)">
                编辑
              </button>
              <button class="op-btn danger" :disabled="saving || uploading" @click="remove(item)">
                删除
              </button>
            </template>
          </td>
        </tr>
        <tr v-if="!list.length">
          <td colspan="6" class="empty">暂无证书，用上方表单上传第一张</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.awards-admin {
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

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: var(--bg-panel);
  border: 2px solid var(--border);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--text-dim);
}

.field.span-2 {
  grid-column: span 2;
}

.field input {
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 7px 10px;
  outline: none;
  caret-color: var(--green);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.field input:focus {
  border-color: var(--green);
  box-shadow: 0 0 12px var(--green-soft);
}

.file-input {
  min-height: 36px;
}

.file-input::file-selector-button {
  margin-right: 10px;
  padding: 5px 12px;
  border: 2px solid var(--border-bright);
  background: transparent;
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 12px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    color 0.2s;
}

.file-input::file-selector-button:hover {
  border-color: var(--green);
  color: var(--green);
}

.actions {
  display: flex;
  gap: 10px;
}

.neon-btn {
  font-size: 11px;
  padding: 10px 20px;
}

.loading {
  margin: 0;
  color: var(--text-dim);
}

.thumb-cell {
  width: 72px;
}

.thumb-img {
  display: block;
  height: 48px;
  width: auto;
  max-width: 64px;
  border: 1px solid var(--border);
  object-fit: cover;
}

.pdf-thumb {
  width: 64px;
  background: var(--bg-panel);
  pointer-events: none;
}

.id {
  color: var(--text-dim);
}

.name-cell {
  min-width: 160px;
}

.time-cell {
  white-space: nowrap;
}

.sort-cell {
  white-space: nowrap;
}

.edit-input {
  width: 100%;
  min-width: 90px;
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 5px 8px;
  outline: none;
  caret-color: var(--green);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.edit-input:focus {
  border-color: var(--green);
  box-shadow: 0 0 12px var(--green-soft);
}

.name-edit {
  min-width: 180px;
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

.op-btn.save {
  border-color: var(--green);
  color: var(--green);
}

.op-btn.danger:hover:not(:disabled) {
  border-color: var(--error);
  color: var(--error);
  box-shadow: 0 0 10px var(--error-soft);
}

.op-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.empty {
  color: var(--text-dim);
  text-align: center;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr 1fr;
  }

  .field.span-2 {
    grid-column: span 2;
  }
}

@media (max-width: 520px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .field.span-2 {
    grid-column: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }

  .title {
    text-shadow: none;
  }
}
</style>
