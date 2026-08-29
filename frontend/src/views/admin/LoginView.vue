<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function onSubmit() {
  if (loading.value) return
  if (!username.value || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
    router.push(redirect)
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <form class="terminal" @submit.prevent="onSubmit">
      <div class="terminal-bar">
        <span class="dot red"></span>
        <span class="dot yellow"></span>
        <span class="dot green"></span>
        <span class="terminal-title">whoami-admin — login</span>
      </div>
      <div class="terminal-body">
        <p class="term-line">whoami 管理后台 · 管理员登录</p>

        <label class="term-field">
          <span class="term-label">&gt; username:</span>
          <input
            v-model="username"
            autocomplete="username"
            spellcheck="false"
            :disabled="loading"
          />
        </label>

        <label class="term-field">
          <span class="term-label">&gt; password:</span>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            :disabled="loading"
          />
        </label>

        <p v-if="errorMsg" class="term-error">[error] {{ errorMsg }}</p>

        <button class="term-btn" type="submit" :disabled="loading">
          {{ loading ? '> authenticating ...' : '> login' }}
        </button>

        <p class="term-hint">连续 5 次失败将锁定账号 10 分钟</p>
      </div>
    </form>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
}

.terminal {
  width: 100%;
  max-width: 420px;
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow: 0 0 40px rgba(51, 255, 102, 0.06);
}

.terminal-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.red {
  background: #ff5f56;
}

.dot.yellow {
  background: #ffbd2e;
}

.dot.green {
  background: #27c93f;
}

.terminal-title {
  margin-left: 8px;
  color: var(--text-dim);
  font-size: 12px;
}

.terminal-body {
  padding: 20px 18px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.term-line {
  margin: 0 0 6px;
  color: var(--accent);
}

.term-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.term-label {
  color: var(--text-dim);
  font-size: 12px;
}

.term-field input {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 14px;
  padding: 9px 12px;
  outline: none;
  transition: border-color 0.2s;
}

.term-field input:focus {
  border-color: var(--accent-dim);
}

.term-error {
  margin: 0;
  color: var(--error);
  font-size: 13px;
  word-break: break-all;
}

.term-btn {
  background: transparent;
  border: 1px solid var(--accent-dim);
  border-radius: 4px;
  color: var(--accent);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 14px;
  padding: 10px 16px;
  transition:
    background 0.2s,
    box-shadow 0.2s;
}

.term-btn:hover:not(:disabled) {
  background: rgba(51, 255, 102, 0.08);
  box-shadow: 0 0 12px rgba(51, 255, 102, 0.25);
}

.term-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.term-hint {
  margin: 0;
  color: var(--text-dim);
  font-size: 12px;
}
</style>
