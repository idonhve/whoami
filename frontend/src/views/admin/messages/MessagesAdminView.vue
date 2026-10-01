<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  deleteMessage,
  fetchAdminMessages,
  replyMessage,
  updateMessageStatus,
  type AdminMessageItem,
  type MessageStatus,
} from '@/api/message'

/**
 * 留言管理（Spec 05 后台）：状态筛选（全部/approved/hidden）、脱敏 IP 展示、
 * 回复（PUT reply，可空=清除）、下架/恢复（PUT status）、删除（确认弹窗）。
 */

type StatusFilter = 'all' | MessageStatus

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'approved', label: '已通过' },
  { value: 'hidden', label: '已下架' },
]

const list = ref<AdminMessageItem[]>([])
const filter = ref<StatusFilter>('all')
const loading = ref(true)
const errorMsg = ref('')
const actionMsg = ref('')

/** 行内回复编辑：正在编辑的留言 id → 草稿 */
const replyDraft = ref<number | null>(null)
const replyText = ref('')
const replying = ref(false)

/** 删除确认弹窗 */
const deleteTarget = ref<AdminMessageItem | null>(null)
const deleting = ref(false)

/** 展开查看完整内容的留言 id */
const expanded = ref<Set<number>>(new Set())

const filteredCount = computed(() => list.value.length)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    list.value = await fetchAdminMessages(filter.value === 'all' ? undefined : filter.value)
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function setFilter(next: StatusFilter) {
  if (filter.value === next) return
  filter.value = next
  load()
}

function toggleExpand(id: number) {
  const next = new Set(expanded.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expanded.value = next
}

function isLong(item: AdminMessageItem): boolean {
  return item.content.length > 60
}

function contentText(item: AdminMessageItem): string {
  if (!isLong(item) || expanded.value.has(item.id)) return item.content
  return `${item.content.slice(0, 60)}…`
}

/* ---- 回复 ---- */
function startReply(item: AdminMessageItem) {
  replyDraft.value = item.id
  replyText.value = item.reply ?? ''
}

function cancelReply() {
  replyDraft.value = null
  replyText.value = ''
}

async function saveReply(item: AdminMessageItem) {
  replying.value = true
  actionMsg.value = ''
  try {
    await replyMessage(item.id, replyText.value.trim())
    cancelReply()
    await load()
  } catch (error) {
    actionMsg.value = error instanceof Error ? `[error] ${error.message}` : '[error] 回复失败'
  } finally {
    replying.value = false
  }
}

/* ---- 下架 / 恢复 ---- */
async function toggleStatus(item: AdminMessageItem) {
  actionMsg.value = ''
  try {
    await updateMessageStatus(item.id, item.status === 'approved' ? 'hidden' : 'approved')
    await load()
  } catch (error) {
    actionMsg.value = error instanceof Error ? `[error] ${error.message}` : '[error] 操作失败'
  }
}

/* ---- 删除（确认弹窗） ---- */
function askDelete(item: AdminMessageItem) {
  deleteTarget.value = item
}

function cancelDelete() {
  deleteTarget.value = null
}

async function confirmDelete() {
  const target = deleteTarget.value
  if (!target || deleting.value) return
  deleting.value = true
  try {
    await deleteMessage(target.id)
    deleteTarget.value = null
    await load()
  } catch (error) {
    actionMsg.value = error instanceof Error ? `[error] ${error.message}` : '[error] 删除失败'
  } finally {
    deleting.value = false
  }
}

function fmtTime(iso: string | null): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(load)
</script>

<template>
  <section class="messages-page">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        MESSAGES
      </h1>
      <p class="sub">$ messages --list · 回复 / 下架 / 删除 · IP 已脱敏</p>
      <div class="filter" role="group" aria-label="留言状态筛选">
        <button
          v-for="opt in STATUS_FILTERS"
          :key="opt.value"
          type="button"
          class="filter-btn"
          :class="{ active: filter === opt.value }"
          @click="setFilter(opt.value)"
        >
          {{ opt.label }}
        </button>
        <span class="count dim">{{ filteredCount }} 条</span>
      </div>
    </header>

    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>
    <p v-if="actionMsg" class="action-msg" role="status">{{ actionMsg }}</p>
    <p v-if="loading" class="loading dim">loading ...</p>

    <table v-else class="term-table">
      <thead>
        <tr>
          <th scope="col">时间</th>
          <th scope="col">昵称</th>
          <th scope="col">邮箱</th>
          <th scope="col">内容</th>
          <th scope="col">IP（脱敏）</th>
          <th scope="col">状态</th>
          <th scope="col">回复</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="item in list" :key="item.id">
          <tr>
            <td class="dim time">{{ fmtTime(item.createdAt) }}</td>
            <td class="nick">{{ item.nickname }}</td>
            <td class="dim">{{ item.email ?? '-' }}</td>
            <td class="content-cell">
              <span>{{ contentText(item) }}</span>
              <button
                v-if="isLong(item)"
                type="button"
                class="expand-btn"
                @click="toggleExpand(item.id)"
              >
                {{ expanded.has(item.id) ? '收起' : '展开' }}
              </button>
            </td>
            <td class="dim ip">{{ item.ip }}</td>
            <td>
              <span class="status" :class="item.status === 'approved' ? 'ok' : 'off'">
                {{ item.status === 'approved' ? 'approved' : 'hidden' }}
              </span>
            </td>
            <td class="dim reply-cell">{{ item.reply ?? '-' }}</td>
            <td class="ops">
              <button type="button" class="op-btn" @click="startReply(item)">回复</button>
              <button type="button" class="op-btn" @click="toggleStatus(item)">
                {{ item.status === 'approved' ? '下架' : '恢复' }}
              </button>
              <button type="button" class="op-btn danger" @click="askDelete(item)">删除</button>
            </td>
          </tr>
          <tr v-if="replyDraft === item.id">
            <td colspan="8" class="reply-editor">
              <label class="reply-label" :for="`reply-${item.id}`">回复 {{ item.nickname }}（留空 = 清除回复）</label>
              <textarea
                :id="`reply-${item.id}`"
                v-model="replyText"
                class="reply-input"
                rows="3"
                maxlength="500"
                placeholder="写下你的回复（≤500 字符）"
              ></textarea>
              <div class="reply-actions">
                <button type="button" class="op-btn primary" :disabled="replying" @click="saveReply(item)">
                  {{ replying ? 'saving ...' : '保存回复' }}
                </button>
                <button type="button" class="op-btn" @click="cancelReply">取消</button>
              </div>
            </td>
          </tr>
        </template>
        <tr v-if="list.length === 0">
          <td colspan="8" class="dim empty">暂无留言</td>
        </tr>
      </tbody>
    </table>

    <!-- 删除确认弹窗（自制终端风，避免引 dialog 组件） -->
    <Teleport to="body">
      <div v-if="deleteTarget" class="modal-mask" @click.self="cancelDelete">
        <div class="modal" role="alertdialog" aria-modal="true" aria-label="删除确认">
          <p class="modal-title">rm message #{{ deleteTarget.id }} ?</p>
          <p class="modal-body dim">
            “{{ deleteTarget.nickname }}” 的留言将被永久删除，不可恢复。确认？
          </p>
          <div class="modal-actions">
            <button type="button" class="op-btn danger" :disabled="deleting" @click="confirmDelete">
              {{ deleting ? 'deleting ...' : '确认删除' }}
            </button>
            <button type="button" class="op-btn" @click="cancelDelete">取消</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.messages-page {
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
}

.filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-btn {
  min-height: 44px;
  padding: 6px 16px;
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text-dim);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 13px;
  transition:
    border-color 0.2s,
    color 0.2s,
    box-shadow 0.2s;
}

.filter-btn:hover {
  border-color: var(--cyan);
  color: var(--cyan);
}

.filter-btn.active {
  border-color: var(--green);
  color: var(--green);
  box-shadow: 0 0 10px var(--green-soft);
}

.count {
  margin-left: 8px;
  font-size: 12px;
}

.error {
  margin: 0;
  color: var(--error);
  font-size: 13px;
}

.action-msg {
  margin: 0;
  color: var(--amber);
  font-size: 13px;
}

.dim {
  color: var(--text-dim);
  font-size: 12px;
}

.loading {
  margin: 0;
}

.time {
  white-space: nowrap;
}

.nick {
  color: var(--cyan);
  font-size: 13px;
}

.content-cell {
  max-width: 320px;
}

.expand-btn {
  margin-left: 8px;
  padding: 0;
  background: transparent;
  border: none;
  color: var(--cyan);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
}

.ip {
  white-space: nowrap;
}

.status {
  font-size: 11px;
  letter-spacing: 1px;
  padding: 2px 8px;
  border: 1px solid currentColor;
}

.status.ok {
  color: var(--green);
}

.status.off {
  color: var(--amber);
}

.reply-cell {
  max-width: 180px;
}

.ops {
  white-space: nowrap;
}

.op-btn {
  min-height: 44px;
  padding: 4px 12px;
  margin-right: 6px;
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  transition:
    border-color 0.2s,
    color 0.2s;
}

.op-btn:hover:not(:disabled) {
  border-color: var(--green);
  color: var(--green);
}

.op-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.op-btn.danger {
  color: var(--error);
  border-color: var(--border-bright);
}

.op-btn.danger:hover:not(:disabled) {
  border-color: var(--error);
}

.op-btn.primary {
  color: var(--green);
}

.op-btn.primary:hover:not(:disabled) {
  border-color: var(--green);
}

.reply-editor {
  background: var(--bg);
}

.reply-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--text-dim);
}

.reply-input {
  width: 100%;
  padding: 10px 12px;
  border: 2px solid var(--border-bright);
  background: var(--bg-panel);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  resize: vertical;
}

.reply-input:focus {
  outline: none;
  border-color: var(--green);
  box-shadow: 0 0 10px var(--green-soft);
}

.reply-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.empty {
  text-align: center;
  padding: 24px 0;
}

/* ---- 删除确认弹窗 ---- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--bg) 72%, transparent);
}

.modal {
  width: min(420px, 92vw);
  padding: 20px;
  border: 2px solid var(--error);
  background: var(--bg-panel);
  box-shadow: 0 0 24px var(--error-soft);
}

.modal-title {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 15px;
  color: var(--error);
}

.modal-body {
  margin: 10px 0 0;
  font-size: 13px;
}

.modal-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }
}
</style>
