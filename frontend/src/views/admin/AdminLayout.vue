<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => {
  auth.fetchMe().catch(() => {
    // 401 已由 http 客户端统一处理跳转
  })
})

function onLogout() {
  auth.logout()
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-shell">
    <header class="admin-header">
      <div class="left">
        <span class="logo">whoami<span class="logo-dim">/admin</span></span>
        <span class="status">
          <span class="led" aria-hidden="true"></span>
          ONLINE
        </span>
      </div>
      <div class="right">
        <span v-if="auth.admin" class="user">@{{ auth.admin.username }}</span>
        <button class="logout" @click="onLogout">&gt; logout</button>
      </div>
    </header>
    <main class="admin-body">
      <RouterView />
    </main>
    <footer class="admin-footer">
      <span>whoami admin console // M1</span>
      <span>PWR ● ● ●</span>
    </footer>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle, rgba(43, 217, 255, 0.03) 1px, transparent 1px);
  background-size: 30px 30px;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  border-bottom: 2px solid var(--border-bright);
  background: var(--bg-panel);
  box-shadow: 0 2px 0 rgba(0, 255, 156, 0.08);
  flex-wrap: wrap;
}

.left,
.right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.logo {
  font-family: var(--font-pixel);
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--green);
  text-shadow: 0 0 8px var(--green-glow);
}

.logo-dim {
  color: var(--text-dim);
  text-shadow: none;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--green);
  font-size: 11px;
  letter-spacing: 2px;
}

.led {
  width: 8px;
  height: 8px;
  background: var(--green);
  box-shadow: 0 0 8px var(--green-glow);
  animation: led-pulse 2.4s ease-in-out infinite;
}

.user {
  color: var(--cyan);
  font-size: 13px;
}

.logout {
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

.logout:hover {
  border-color: var(--error);
  color: var(--error);
  box-shadow: 0 0 12px rgba(255, 56, 96, 0.2);
}

.admin-body {
  flex: 1;
  padding: 28px 24px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

.admin-footer {
  display: flex;
  justify-content: space-between;
  padding: 10px 24px;
  border-top: 2px solid var(--border);
  color: var(--text-dim);
  font-size: 11px;
  letter-spacing: 1px;
}
</style>
