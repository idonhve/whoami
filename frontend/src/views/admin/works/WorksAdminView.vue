<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  fetchAdminWorks,
  fetchSyncLogs,
  triggerSync,
  updateWork,
  type SyncLogItem,
  type SyncResult,
  type WorkAdminItem,
} from '@/api/works'

/**
 * 作品管理页（Spec 04）：列表 + 筛选（语言/置顶/隐藏）+ 行内编辑中文描述
 * + 置顶/隐藏切换（置顶上限 3，超出 409）+ 立即同步 + 同步日志。
 */
const items = ref<WorkAdminItem[]>([])
const loading = ref(true)
const errorMsg = ref('')
const notice = ref('')

const filterLanguage = ref('')
const filterPinned = ref('')
const filterHidden = ref('')

const languages = computed(() => {
  const set = new Set<string>()
  for (const item of items.value) {
    if (item.language) set.add(item.language)
  }
  return [...set].sort()
})

const filtered = computed(() =>
  items.value.filter((item) => {
    if (filterLanguage.value && item.language !== filterLanguage.value) return false
    if (filterPinned.value === 'yes' && !item.isPinned) return false
    if (filterPinned.value === 'no' && item.isPinned) return false
    if (filterHidden.value === 'yes' && !item.isHidden) return false
    if (filterHidden.value === 'no' && item.isHidden) return false
    return true
  }),
)

const editingId = ref<number | null>(null)
const editingValue = ref('')
const saving = ref(false)

const syncing = ref(false)
const syncResult = ref<SyncResult | null>(null)

const logs = ref<SyncLogItem[]>([])
const logsLoading = ref(false)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    items.value = await fetchAdminWorks()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  logsLoading.value = true
  try {
    logs.value = await fetchSyncLogs(20)
  } catch {
    // 日志加载失败不打断主列表
  } finally {
    logsLoading.value = false
  }
}

function startEdit(item: WorkAdminItem) {
  editingId.value = item.id
  editingValue.value = item.cnTitle ?? ''
  notice.value = ''
  errorMsg.value = ''
}

function cancelEdit() {
  editingId.value = null
}

async function saveCnTitle(item: WorkAdminItem) {
  if (saving.value) return
  saving.value = true
  errorMsg.value = ''
  try {
    await updateWork(item.id, { cnTitle: editingValue.value })
    item.cnTitle = editingValue.value
    editingId.value = null
    notice.value = `[ok] ${item.repoName} 中文描述已保存，前台即时生效`
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function togglePin(item: WorkAdminItem) {
  errorMsg.value = ''
  notice.value = ''
  try {
    await updateWork(item.id, { isPinned: !item.isPinned })
    item.isPinned = !item.isPinned
    notice.value = item.isPinned ? `[ok] ${item.repoName} 已置顶` : `[ok] ${item.repoName} 已取消置顶`
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '操作失败'
  }
}

async function toggleHidden(item: WorkAdminItem) {
  errorMsg.value = ''
  notice.value = ''
  try {
    await updateWork(item.id, { isHidden: !item.isHidden })
    item.isHidden = !item.isHidden
    notice.value = item.isHidden ? `[ok] ${item.repoName} 已隐藏` : `[ok] ${item.repoName} 已恢复展示`
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '操作失败'
  }
}

async function sync() {
  if (syncing.value) return
  syncing.value = true
  syncResult.value = null
  errorMsg.value = ''
  notice.value = ''
  try {
    syncResult.value = await triggerSync()
    if (syncResult.value.status === 'success') {
      notice.value = `[sync] ${syncResult.value.repoCount} repos, ${syncResult.value.hiddenGone} hidden-gone`
      await load()
    }
    await loadLogs()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '同步请求失败'
  } finally {
    syncing.value = false
  }
}

function fmtTime(iso: string | null) {
  return iso ? iso.replace('T', ' ').slice(0, 19) : '-'
}

function fmtStatus(status: string) {
  return status === 'success' ? '[ok]' : '[fail]'
}

onMounted(() => {
  void load()
  void loadLogs()
})
</script>

<template>
  <section class="works-admin">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        WORKS / SYNC
      </h1>
      <p class="sub">$ github sync --daily-03:00 · 运营字段（描述/置顶/隐藏）不被同步覆盖</p>
    </header>

    <div class="sync-bar">
      <button class="sync-btn" :disabled="syncing" @click="sync">
        {{ syncing ? 'syncing ...' : '> 立即同步' }}
      </button>
      <p v-if="syncResult" class="sync-result" :class="syncResult.status" role="status">
        {{ fmtStatus(syncResult.status) }} {{ syncResult.status === 'success'
          ? `同步完成：${syncResult.repoCount} 个仓库${syncResult.hiddenGone ? `，${syncResult.hiddenGone} 个已隐藏（远端消失）` : ''}`
          : `同步失败：${syncResult.message}` }}
      </p>
    </div>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>

    <div class="filters">
      <label class="filter">
        <span class="filter-label">语言</span>
        <select v-model="filterLanguage" class="filter-select">
          <option value="">全部</option>
          <option v-for="lang in languages" :key="lang" :value="lang">{{ lang }}</option>
        </select>
      </label>
      <label class="filter">
        <span class="filter-label">置顶</span>
        <select v-model="filterPinned" class="filter-select">
          <option value="">全部</option>
          <option value="yes">是</option>
          <option value="no">否</option>
        </select>
      </label>
      <label class="filter">
        <span class="filter-label">隐藏</span>
        <select v-model="filterHidden" class="filter-select">
          <option value="">全部</option>
          <option value="yes">是</option>
          <option value="no">否</option>
        </select>
      </label>
      <span class="filter-count">{{ filtered.length }} / {{ items.length }}</span>
    </div>

    <p v-if="loading" class="loading">loading ...</p>

    <table v-else class="term-table">
      <thead>
        <tr>
          <th scope="col">REPO</th>
          <th scope="col">中文描述</th>
          <th scope="col">语言</th>
          <th scope="col">★/⑂</th>
          <th scope="col">更新</th>
          <th scope="col">状态</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in filtered" :key="item.id" :class="{ 'row-hidden': item.isHidden }">
          <td class="repo">
            <a :href="item.htmlUrl" target="_blank" rel="noopener noreferrer" class="repo-link">
              {{ item.repoName }}
            </a>
          </td>
          <td class="cn">
            <template v-if="editingId === item.id">
              <input
                v-model="editingValue"
                class="cn-input"
                :disabled="saving"
                maxlength="200"
                spellcheck="false"
                placeholder="中文描述（空则前台回退仓库 description）"
                @keyup.enter="saveCnTitle(item)"
                @keyup.esc="cancelEdit"
              />
            </template>
            <template v-else>
              <span :class="{ 'cn-empty': !item.cnTitle }">{{ item.cnTitle || '(未设置，回退英文描述)' }}</span>
            </template>
          </td>
          <td class="lang">{{ item.language ?? '-' }}</td>
          <td class="stat">{{ item.stargazersCount }} / {{ item.forksCount }}</td>
          <td class="time">{{ fmtTime(item.pushedAt) }}</td>
          <td class="badges">
            <span v-if="item.isPinned" class="badge pin">PIN</span>
            <span v-if="item.isHidden" class="badge hidden">HIDDEN</span>
            <span v-if="!item.isPinned && !item.isHidden" class="badge none">-</span>
          </td>
          <td class="ops">
            <template v-if="editingId === item.id">
              <button class="op-btn save" :disabled="saving" @click="saveCnTitle(item)">
                {{ saving ? '...' : '保存' }}
              </button>
              <button class="op-btn" :disabled="saving" @click="cancelEdit">取消</button>
            </template>
            <template v-else>
              <button class="op-btn" @click="startEdit(item)">编辑</button>
              <button class="op-btn" :class="{ active: item.isPinned }" @click="togglePin(item)">
                {{ item.isPinned ? '取消置顶' : '置顶' }}
              </button>
              <button class="op-btn" :class="{ active: item.isHidden }" @click="toggleHidden(item)">
                {{ item.isHidden ? '恢复' : '隐藏' }}
              </button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>

    <h2 class="logs-title">SYNC LOGS</h2>
    <p v-if="logsLoading" class="loading">loading logs ...</p>
    <table v-else class="term-table logs-table">
      <thead>
        <tr>
          <th scope="col">时间</th>
          <th scope="col">触发</th>
          <th scope="col">状态</th>
          <th scope="col">REPOS</th>
          <th scope="col">HIDDEN-GONE</th>
          <th scope="col">原因</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="logs.length === 0">
          <td colspan="6" class="empty-cell">暂无同步记录 —— 点击「立即同步」生成首条日志</td>
        </tr>
        <tr v-for="entry in logs" :key="entry.id">
          <td class="time">{{ fmtTime(entry.startedAt) }}</td>
          <td class="trigger">{{ entry.triggerType }}</td>
          <td class="status" :class="entry.status">{{ fmtStatus(entry.status) }}</td>
          <td class="stat">{{ entry.repoCount }}</td>
          <td class="stat">{{ entry.hiddenGone }}</td>
          <td class="reason">{{ entry.message ?? '-' }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.works-admin {
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

.sync-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
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
  cursor: wait;
  opacity: 0.55;
}

.sync-result {
  margin: 0;
  font-size: 13px;
}

.sync-result.success {
  color: var(--green);
}

.sync-result.failed {
  color: var(--error);
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

.filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  color: var(--text-dim);
  font-size: 12px;
}

.filter-select {
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 6px 10px;
  min-height: 36px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
}

.filter-select:focus {
  border-color: var(--green);
}

.filter-count {
  margin-left: auto;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 16px;
}

.repo-link {
  color: var(--cyan);
  white-space: nowrap;
}

.row-hidden {
  opacity: 0.5;
}

.cn-input {
  width: 100%;
  min-width: 200px;
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

.cn-input:focus {
  border-color: var(--green);
  box-shadow: 0 0 12px var(--green-soft);
}

.cn-empty {
  color: var(--text-dim);
}

.lang {
  white-space: nowrap;
}

.stat {
  white-space: nowrap;
}

.time {
  color: var(--text-dim);
  font-size: 12px;
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

.badge.pin {
  color: var(--amber);
  border-color: var(--amber);
}

.badge.hidden {
  color: var(--error);
  border-color: var(--error);
}

.badge.none {
  border-color: var(--border);
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

.op-btn.active {
  border-color: var(--amber);
  color: var(--amber);
}

.op-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.logs-title {
  margin: 16px 0 0;
  font-family: var(--font-pixel);
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 2px;
  color: var(--cyan);
}

.logs-table .status.success {
  color: var(--green);
}

.logs-table .status.failed {
  color: var(--error);
}

.trigger {
  color: var(--cyan);
}

.reason {
  color: var(--text-dim);
  word-break: break-all;
}

.empty-cell {
  color: var(--text-dim);
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }
}
</style>
