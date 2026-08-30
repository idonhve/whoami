<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  fetchAdminSiteConfigs,
  updateSiteConfig,
  type SiteConfigItem,
} from '@/api/admin/config'

const items = ref<SiteConfigItem[]>([])
const loading = ref(true)
const errorMsg = ref('')
const notice = ref('')

const editingKey = ref<string | null>(null)
const editingValue = ref('')
const saving = ref(false)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    items.value = await fetchAdminSiteConfigs()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function startEdit(item: SiteConfigItem) {
  editingKey.value = item.key
  editingValue.value = item.value ?? ''
  notice.value = ''
  errorMsg.value = ''
}

function cancelEdit() {
  editingKey.value = null
}

async function saveEdit(key: string) {
  if (saving.value) return
  saving.value = true
  errorMsg.value = ''
  try {
    await updateSiteConfig(key, editingValue.value)
    editingKey.value = null
    notice.value = `[ok] ${key} 已保存，公开接口即时生效`
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function fmtTime(iso: string | null) {
  return iso ? iso.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>

<template>
  <section class="config-page">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        SITE CONFIG
      </h1>
      <p class="sub">$ site-config --list · 白名单键经 GET /api/site-config 下发前台，其余键仅后台可见</p>
    </header>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>

    <p v-if="loading" class="loading">loading ...</p>

    <table v-else class="term-table">
      <thead>
        <tr>
          <th scope="col">KEY</th>
          <th scope="col">VALUE</th>
          <th scope="col">说明</th>
          <th scope="col">更新时间</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.key">
          <td class="key">{{ item.key }}</td>
          <td class="value">
            <template v-if="editingKey === item.key">
              <input
                v-model="editingValue"
                class="value-input"
                :disabled="saving"
                spellcheck="false"
                @keyup.enter="saveEdit(item.key)"
                @keyup.esc="cancelEdit"
              />
            </template>
            <template v-else>{{ item.value === '' || item.value === null ? '(空)' : item.value }}</template>
          </td>
          <td class="desc">{{ item.description ?? '-' }}</td>
          <td class="time">{{ fmtTime(item.updatedAt) }}</td>
          <td class="ops">
            <template v-if="editingKey === item.key">
              <button class="op-btn save" :disabled="saving" @click="saveEdit(item.key)">
                {{ saving ? '...' : '保存' }}
              </button>
              <button class="op-btn" :disabled="saving" @click="cancelEdit">取消</button>
            </template>
            <button v-else class="op-btn" @click="startEdit(item)">编辑</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.config-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

.loading {
  margin: 0;
  color: var(--text-dim);
}

.key {
  color: var(--cyan);
  white-space: nowrap;
}

.value {
  min-width: 160px;
}

.value-input {
  width: 100%;
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 6px 10px;
  outline: none;
  caret-color: var(--green);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.value-input:focus {
  border-color: var(--green);
  box-shadow: 0 0 12px var(--green-soft);
}

.desc {
  color: var(--text-dim);
  font-size: 12px;
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

.op-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }
}
</style>
