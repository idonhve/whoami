<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

/**
 * 前台悬浮「管理」按钮（Spec 06）：
 * 仅当本地存在 token 且 GET /admin/api/auth/me 校验通过时渲染；
 * 访客（无 token / token 失效）看不到任何后台入口。后台页面自身不显示。
 */
const auth = useAuthStore()
const route = useRoute()

const verified = ref(false)
let checking = false

watch(
  () => auth.token,
  async (token) => {
    verified.value = false
    if (!token || checking) return
    checking = true
    try {
      await auth.fetchMe()
      verified.value = true
    } catch {
      // 401 由 http 客户端统一清理会话；这里只需保持隐藏
      verified.value = false
    } finally {
      checking = false
    }
  },
  { immediate: true },
)

const visible = computed(
  () => Boolean(auth.token) && verified.value && !route.path.startsWith('/admin'),
)
</script>

<template>
  <RouterLink v-if="visible" to="/admin" class="floating-admin-btn" aria-label="进入管理后台">
    <svg class="fab-icon" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M2 1 L8 6 L2 11 L2 9 L5 6 L2 3 Z M7 12 H14 V14 H7 Z" fill="currentColor" />
    </svg>
    <span class="fab-label">ADMIN</span>
    <span class="fab-led" aria-hidden="true"></span>
  </RouterLink>
</template>

<style scoped>
.floating-admin-btn {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 9000;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 44px;
  min-height: 44px;
  padding: 10px 16px;
  background: var(--bg-raised);
  border: 2px solid var(--green);
  color: var(--green);
  box-shadow:
    0 0 16px var(--green-glow),
    inset 0 0 10px var(--green-soft);
  text-shadow: 0 0 6px var(--green-glow);
  transition:
    box-shadow 0.2s,
    transform 0.15s;
}

.floating-admin-btn:hover {
  box-shadow:
    0 0 26px var(--green-glow),
    inset 0 0 14px var(--green-soft);
  transform: translateY(-2px);
}

.fab-icon {
  width: 14px;
  height: 14px;
}

.fab-label {
  font-family: var(--font-pixel);
  font-size: 9px;
  letter-spacing: 1px;
}

.fab-led {
  width: 6px;
  height: 6px;
  background: var(--green);
  box-shadow: 0 0 8px var(--green-glow);
  animation: led-pulse 2.4s ease-in-out infinite;
}

@media (max-width: 720px) {
  .floating-admin-btn {
    right: 16px;
    bottom: 16px;
  }

  .fab-label {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .fab-led {
    animation: none;
  }

  .floating-admin-btn:hover {
    transform: none;
  }
}
</style>
