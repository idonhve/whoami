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
    <div class="hud-frame"></div>

    <form class="terminal" @submit.prevent="onSubmit">
      <div class="terminal-bar">
        <span class="dot red"></span>
        <span class="dot yellow"></span>
        <span class="dot green"></span>
        <span class="terminal-title">whoami-admin — login</span>
        <span class="tty">TTY/01</span>
      </div>
      <div class="terminal-body">
        <p class="boot">AUTH MODULE v2.1 // 身份验证</p>

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
  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background:
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(0, 255, 156, 0.05), transparent 70%),
    radial-gradient(circle, rgba(43, 217, 255, 0.04) 1px, transparent 1px);
  background-size:
    100% 100%,
    30px 30px;
}

.terminal {
  position: relative;
  width: 100%;
  max-width: 430px;
  background: var(--bg-panel);
  border: 2px solid var(--border-bright);
  box-shadow:
    0 0 0 1px var(--bg),
    0 0 30px rgba(0, 255, 156, 0.07),
    0 24px 60px rgba(0, 0, 0, 0.5);
  animation: rise-in 0.4s ease-out both;
}

.terminal-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 14px;
  border-bottom: 2px solid var(--border-bright);
  background: var(--bg-raised);
}

.dot {
  width: 10px;
  height: 10px;
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
  letter-spacing: 0.5px;
}

.tty {
  margin-left: auto;
  color: var(--border-bright);
  font-family: var(--font-pixel);
  font-size: 8px;
}

.terminal-body {
  padding: 22px 20px 26px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.boot {
  margin: 0 0 4px;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 20px;
  letter-spacing: 0.5px;
}

.term-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.term-label {
  color: var(--text-dim);
  font-size: 12px;
  letter-spacing: 1px;
}

.term-field input {
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 14px;
  padding: 10px 12px;
  outline: none;
  caret-color: var(--green);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.term-field input:focus {
  border-color: var(--green);
  box-shadow:
    0 0 14px rgba(0, 255, 156, 0.18),
    inset 0 0 8px rgba(0, 255, 156, 0.04);
  text-shadow: 0 0 6px var(--green-glow);
}

.term-field input:disabled {
  opacity: 0.5;
}

.term-error {
  margin: 0;
  color: var(--error);
  font-size: 13px;
  word-break: break-all;
  text-shadow: 0 0 8px rgba(255, 56, 96, 0.4);
  animation: error-shake 0.3s ease-out;
}

@keyframes error-shake {
  25% {
    transform: translateX(-4px);
  }
  50% {
    transform: translateX(3px);
  }
  75% {
    transform: translateX(-2px);
  }
}

.term-btn {
  background: rgba(0, 255, 156, 0.05);
  border: 2px solid var(--green);
  color: var(--green);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 14px;
  letter-spacing: 1px;
  padding: 11px 16px;
  text-shadow: 0 0 6px var(--green-glow);
  transition:
    background 0.2s,
    box-shadow 0.2s,
    text-shadow 0.2s;
}

.term-btn:hover:not(:disabled) {
  background: rgba(0, 255, 156, 0.13);
  box-shadow:
    0 0 20px rgba(0, 255, 156, 0.3),
    inset 0 0 14px rgba(0, 255, 156, 0.08);
  text-shadow:
    2px 0 var(--magenta),
    -2px 0 var(--cyan),
    0 0 10px var(--green-glow);
}

.term-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.term-hint {
  margin: 0;
  color: var(--text-dim);
  font-size: 12px;
}
</style>
