<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { fetchOpLogs, type OpLogItem } from '@/api/admin/oplog'

const PAGE_SIZE = 20

const list = ref<OpLogItem[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(true)
const errorMsg = ref('')
const expanded = ref<Set<number>>(new Set())

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchOpLogs(page.value, PAGE_SIZE)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function goPage(target: number) {
  if (target < 1 || target > totalPages.value || target === page.value) return
  page.value = target
  expanded.value = new Set()
  load()
}

function toggle(id: number) {
  const next = new Set(expanded.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expanded.value = next
}

function detailText(item: OpLogItem) {
  return item.detail ? JSON.stringify(item.detail, null, 2) : '(无参数)'
}

function fmtTime(iso: string) {
  return iso ? iso.replace('T', ' ').slice(0, 19) : '-'
}

function actionClass(action: string) {
  if (action === 'LOGIN') return 'login'
  if (action === 'DELETE') return 'delete'
  return 'write'
}

onMounted(load)
</script>

<template>
  <section class="oplog-page">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        OP LOG
      </h1>
      <p class="sub">$ op-logs --tail · 全部 /admin/api 写操作与登录行为自动留痕，共 {{ total }} 条</p>
    </header>

    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>
    <p v-if="loading" class="loading">loading ...</p>

    <template v-else>
      <table class="term-table">
        <thead>
          <tr>
            <th scope="col">时间</th>
            <th scope="col">操作者</th>
            <th scope="col">动作</th>
            <th scope="col">资源</th>
            <th scope="col">资源 ID</th>
            <th scope="col">IP</th>
            <th scope="col">参数摘要</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td class="time">{{ fmtTime(item.createdAt) }}</td>
            <td class="dim">#{{ item.adminUserId }}</td>
            <td>
              <span class="action" :class="actionClass(item.action)">{{ item.action }}</span>
            </td>
            <td class="resource">{{ item.resource }}</td>
            <td class="dim">{{ item.resourceId ?? '-' }}</td>
            <td class="dim">{{ item.ip }}</td>
            <td class="detail">
              <button class="detail-toggle" @click="toggle(item.id)">
                {{ expanded.has(item.id) ? '收起' : '展开' }}
              </button>
              <pre v-if="expanded.has(item.id)" class="detail-json">{{ detailText(item) }}</pre>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="7" class="dim empty">暂无操作日志</td>
          </tr>
        </tbody>
      </table>

      <nav class="pager" aria-label="日志分页">
        <button class="pg-btn" :disabled="page <= 1" @click="goPage(page - 1)">&lt; prev</button>
        <span class="pg-info">{{ page }} / {{ totalPages }}</span>
        <button class="pg-btn" :disabled="page >= totalPages" @click="goPage(page + 1)">next &gt;</button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.oplog-page {
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

.error {
  margin: 0;
  color: var(--error);
  font-size: 13px;
}

.loading {
  margin: 0;
  color: var(--text-dim);
}

.dim {
  color: var(--text-dim);
  font-size: 12px;
}

.time {
  color: var(--text-dim);
  font-size: 12px;
  white-space: nowrap;
}

.resource {
  font-size: 12px;
  word-break: break-all;
}

.action {
  font-size: 11px;
  letter-spacing: 1px;
  padding: 2px 8px;
  border: 1px solid currentColor;
}

.action.login {
  color: var(--cyan);
}

.action.write {
  color: var(--green);
}

.action.delete {
  color: var(--error);
}

.detail {
  min-width: 90px;
}

.detail-toggle {
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text-dim);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 3px 10px;
  transition:
    border-color 0.2s,
    color 0.2s;
}

.detail-toggle:hover {
  border-color: var(--cyan);
  color: var(--cyan);
}

.detail-json {
  margin: 8px 0 0;
  padding: 10px;
  background: var(--bg);
  border: 1px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.5;
  max-width: 360px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.empty {
  text-align: center;
  padding: 24px 0;
}

.pager {
  display: flex;
  align-items: center;
  gap: 16px;
}

.pg-btn {
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 6px 14px;
  transition:
    border-color 0.2s,
    color 0.2s,
    box-shadow 0.2s;
}

.pg-btn:hover:not(:disabled) {
  border-color: var(--green);
  color: var(--green);
  box-shadow: 0 0 10px var(--green-soft);
}

.pg-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.pg-info {
  color: var(--text-dim);
  font-size: 12px;
  letter-spacing: 1px;
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }
}
</style>
