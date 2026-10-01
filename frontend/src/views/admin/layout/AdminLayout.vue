<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { adminNavItems } from './registry'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => {
  auth.fetchMe().catch(() => {
    // 401 已由 http 客户端统一处理（续签 / 跳登录）
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
        <RouterLink :to="{ name: 'admin-home' }" class="logo">
          whoami<span class="logo-dim">/admin</span>
        </RouterLink>
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

    <div class="admin-main">
      <aside class="admin-side">
        <nav class="side-nav" aria-label="后台模块导航">
          <RouterLink :to="{ name: 'admin-home' }" class="nav-item" active-class="active" exact-active-class="active">
            <svg class="nav-icon" viewBox="0 0 16 16" aria-hidden="true">
              <path d="M2 2h12v2H2z M2 7h12v2H2z M2 12h8v2H2z" fill="currentColor" />
            </svg>
            <span>总览</span>
          </RouterLink>
          <!-- 模块导航项来自聚合表 registry.ts，新增模块只需在模块目录导出 AdminModule -->
          <RouterLink
            v-for="item in adminNavItems"
            :key="item.name"
            :to="{ name: item.name }"
            class="nav-item"
            active-class="active"
          >
            <svg class="nav-icon" viewBox="0 0 16 16" aria-hidden="true">
              <path :d="item.icon" fill="currentColor" />
            </svg>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
        <p class="side-foot">MODULES: {{ adminNavItems.length }}</p>
      </aside>

      <main class="admin-body">
        <RouterView />
      </main>
    </div>

    <footer class="admin-footer">
      <span>whoami admin console // F6</span>
      <span>PWR ● ● ●</span>
    </footer>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle, var(--cyan-grid) 1px, transparent 1px);
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
  box-shadow: 0 2px 0 var(--green-soft);
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
  box-shadow: 0 0 12px var(--error-soft);
}

.admin-main {
  flex: 1;
  display: flex;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.admin-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 0 24px 24px;
  width: 180px;
  flex-shrink: 0;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  color: var(--text-dim);
  font-size: 13px;
  border: 2px solid transparent;
  transition:
    color 0.2s,
    border-color 0.2s,
    background 0.2s;
}

.nav-item:hover {
  color: var(--text);
}

.nav-item.active {
  color: var(--green);
  border-color: var(--border-bright);
  background: var(--green-soft);
  text-shadow: 0 0 6px var(--green-glow);
}

.nav-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.side-foot {
  margin: 0;
  color: var(--border-bright);
  font-family: var(--font-pixel);
  font-size: 8px;
  letter-spacing: 1px;
  padding: 0 12px;
}

.admin-body {
  flex: 1;
  min-width: 0;
  padding: 28px 24px;
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

@media (max-width: 720px) {
  .admin-main {
    flex-direction: column;
  }

  .admin-side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    padding: 12px 16px 0;
  }

  .side-nav {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .side-foot {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }
}
</style>
