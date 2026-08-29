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
      <span class="title">whoami / admin</span>
      <div class="right">
        <span v-if="auth.admin" class="user">@{{ auth.admin.username }}</span>
        <button class="logout" @click="onLogout">&gt; logout</button>
      </div>
    </header>
    <main class="admin-body">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-panel);
}

.title {
  color: var(--accent);
  font-weight: bold;
}

.right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user {
  color: var(--text-dim);
}

.logout {
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 6px 14px;
  transition:
    border-color 0.2s,
    color 0.2s;
}

.logout:hover {
  border-color: var(--error);
  color: var(--error);
}

.admin-body {
  flex: 1;
  padding: 24px;
}
</style>
