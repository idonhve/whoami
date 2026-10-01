<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  fetchPublicMessages,
  submitMessage,
  type MessageItem,
} from '@/api/message'
import { ApiError } from '@/api/http'
import { trackEvent } from '@/tracker'

/**
 * 留言板（Spec 05）：自愿留名（昵称必填 ≤ 20、内容必填 ≤ 500、邮箱选填格式校验），
 * 前端预校验 + 后端 400 双保险；成功 toast `> message sent ✓`；429 限流提示。
 * 锚点约定：根元素 id="message-board"（命令面板 / 页内跳转引用）。
 */

const MESSAGES_LIMIT = 20

const messages = ref<MessageItem[]>([])
const loading = ref(true)
const loadFailed = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  content: '',
})

const errors = reactive({
  nickname: '',
  email: '',
  content: '',
})

const submitting = ref(false)

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/* ---- toast（终端风，组件内自治；avoidance：App.vue 无 NMessageProvider） ---- */
const toast = reactive({
  visible: false,
  kind: 'success' as 'success' | 'error',
  text: '',
})
let toastTimer: ReturnType<typeof setTimeout> | null = null

function showToast(kind: 'success' | 'error', text: string): void {
  toast.visible = true
  toast.kind = kind
  toast.text = text
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.visible = false
  }, 3200)
}

const toastClass = computed(() => `toast --${toast.kind}`)

/* ---- 列表 ---- */
async function load() {
  loading.value = true
  loadFailed.value = false
  try {
    messages.value = await fetchPublicMessages(MESSAGES_LIMIT)
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function fmtTime(iso: string | null): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : ''
}

/* ---- 前端预校验（后端 400 双保险） ---- */
function validate(): boolean {
  errors.nickname = ''
  errors.email = ''
  errors.content = ''
  const nickname = form.nickname.trim()
  if (!nickname) {
    errors.nickname = '[error] nickname 必填'
  } else if (nickname.length > 20) {
    errors.nickname = '[error] nickname 不能超过 20 字符'
  }
  const email = form.email.trim()
  if (email && !EMAIL_RE.test(email)) {
    errors.email = '[error] email 格式不正确'
  } else if (email.length > 100) {
    errors.email = '[error] email 不能超过 100 字符'
  }
  const content = form.content.trim()
  if (!content) {
    errors.content = '[error] content 必填'
  } else if (content.length > 500) {
    errors.content = '[error] content 不能超过 500 字符'
  }
  return !errors.nickname && !errors.email && !errors.content
}

async function onSubmit() {
  if (submitting.value) return
  if (!validate()) return
  submitting.value = true
  try {
    const email = form.email.trim()
    await submitMessage({
      nickname: form.nickname.trim(),
      content: form.content.trim(),
      ...(email ? { email } : {}),
    })
    trackEvent('message_submit')
    showToast('success', '> message sent ✓')
    form.nickname = ''
    form.email = ''
    form.content = ''
    await load()
  } catch (error) {
    if (error instanceof ApiError && error.status === 429) {
      showToast('error', '[error] 429 · 留言太快了，同 IP 每分钟最多 3 条，稍后再试')
    } else if (error instanceof ApiError) {
      showToast('error', `[error] ${error.message}`)
    } else {
      showToast('error', '[error] 提交失败，请稍后再试')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div id="message-board" class="message-board">
    <header class="head">
      <p class="cmd">$ tail -f /var/log/messages --follow</p>
      <p class="sub">
        <template v-if="loading">loading board ...</template>
        <template v-else-if="loadFailed">[error] 留言板暂时离线</template>
        <template v-else-if="messages.length === 0">0 条留言 · 抢个沙发？</template>
        <template v-else>{{ messages.length }} 条留言 · 昵称必填 / 邮箱选填</template>
      </p>
    </header>

    <ul v-if="!loading && !loadFailed && messages.length > 0" class="list">
      <li v-for="msg in messages" :key="msg.id" class="msg">
        <div class="msg-head">
          <span class="nick">{{ msg.nickname }}</span>
          <span class="time dim">{{ fmtTime(msg.createdAt) }}</span>
        </div>
        <p class="content">{{ msg.content }}</p>
        <div v-if="msg.reply" class="reply">
          <span class="reply-tag">&gt; 站主回复</span>
          <p class="reply-text">{{ msg.reply }}</p>
        </div>
      </li>
    </ul>

    <form class="form" novalidate @submit.prevent="onSubmit">
      <div class="row">
        <label class="field">
          <span class="label">昵称 <em aria-hidden="true">*</em></span>
          <input
            v-model="form.nickname"
            class="input"
            name="nickname"
            type="text"
            maxlength="20"
            placeholder="怎么称呼你（必填，≤20 字符）"
            autocomplete="name"
            @blur="validate"
          />
          <span v-if="errors.nickname" class="field-error" role="alert">{{ errors.nickname }}</span>
        </label>
        <label class="field">
          <span class="label">邮箱 <span class="dim">（选填，仅站主可见）</span></span>
          <input
            v-model="form.email"
            class="input"
            name="email"
            type="email"
            maxlength="100"
            placeholder="hr@example.com（选填）"
            autocomplete="email"
            @blur="validate"
          />
          <span v-if="errors.email" class="field-error" role="alert">{{ errors.email }}</span>
        </label>
      </div>
      <label class="field">
        <span class="label">留言 <em aria-hidden="true">*</em></span>
        <textarea
          v-model="form.content"
          class="input area"
          name="content"
          rows="4"
          maxlength="500"
          placeholder="想对站主说的话（必填，≤500 字符）"
        ></textarea>
        <span class="counter dim">{{ form.content.length }} / 500</span>
        <span v-if="errors.content" class="field-error" role="alert">{{ errors.content }}</span>
      </label>
      <button class="neon-btn send" type="submit" :disabled="submitting">
        <span class="prompt" aria-hidden="true">&gt;</span>
        {{ submitting ? 'sending ...' : 'send message' }}
      </button>
    </form>

    <Teleport to="body">
      <Transition name="toast">
        <div v-if="toast.visible" :class="toastClass" role="status" aria-live="polite">
          {{ toast.text }}
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.message-board {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.head .cmd {
  margin: 0;
  font-family: var(--font-term);
  font-size: 20px;
  color: var(--text-dim);
}

.head .sub {
  margin: 2px 0 0;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-dim);
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg {
  padding: 12px 16px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
}

.msg-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.nick {
  color: var(--cyan);
  font-size: 13px;
  text-shadow: 0 0 8px var(--cyan-soft);
}

.time,
.dim {
  color: var(--text-dim);
  font-size: 12px;
}

.content {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text);
}

.reply {
  margin-top: 10px;
  padding: 8px 12px;
  border-left: 2px solid var(--green);
  background: var(--green-soft);
}

.reply-tag {
  font-size: 12px;
  color: var(--green);
}

.reply-text {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---- 表单 ---- */
.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 720px) {
  .row {
    grid-template-columns: 1fr;
  }
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: var(--text-dim);
  letter-spacing: 1px;
}

.label em {
  color: var(--magenta);
  font-style: normal;
}

.input {
  width: 100%;
  padding: 10px 12px;
  border: 2px solid var(--border-bright);
  background: var(--bg);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 14px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.input:focus {
  outline: none;
  border-color: var(--green);
  box-shadow: 0 0 10px var(--green-soft);
}

.area {
  resize: vertical;
  min-height: 96px;
}

.counter {
  align-self: flex-end;
}

.field-error {
  color: var(--error);
  font-size: 12px;
}

.send {
  align-self: flex-start;
  font-family: var(--font-mono);
}

.send .prompt {
  color: var(--magenta);
  margin-right: 6px;
}

/* ---- toast（fixed 终端条，只动 transform/opacity） ---- */
.toast {
  position: fixed;
  left: 50%;
  bottom: 48px;
  z-index: 10000;
  padding: 12px 22px;
  border: 2px solid var(--green);
  background: var(--bg-raised);
  color: var(--green);
  font-family: var(--font-term);
  font-size: 20px;
  letter-spacing: 1px;
  text-shadow: 0 0 8px var(--green-glow);
  box-shadow: 0 0 18px var(--green-soft);
  pointer-events: none;
}

.toast.--error {
  border-color: var(--error);
  color: var(--error);
  text-shadow: 0 0 8px var(--error-soft);
  box-shadow: 0 0 18px var(--error-soft);
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.24s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}

.toast-enter-to,
.toast-leave-from {
  opacity: 1;
  transform: translate(-50%, 0);
}

/* toast 默认水平定位在中心（transition transform 会覆盖 left 定位） */
.toast {
  transform: translateX(-50%);
}

@media (prefers-reduced-motion: reduce) {
  .toast-enter-active,
  .toast-leave-active {
    transition: none;
  }
}
</style>
