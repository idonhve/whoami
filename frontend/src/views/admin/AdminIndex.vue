<script setup lang="ts">
interface ModuleCard {
  code: string
  title: string
  status: 'ONLINE' | 'PENDING' | 'PHASE-2'
}

const MODULES: ModuleCard[] = [
  { code: 'M1', title: '地基：登录 / 路由 / 部署', status: 'ONLINE' },
  { code: 'F1', title: '欢迎页：开机动画 + Hero', status: 'PENDING' },
  { code: 'F2', title: '技术栈展示', status: 'PENDING' },
  { code: 'F3', title: 'GitHub 图标跳转', status: 'PENDING' },
  { code: 'F4', title: 'GitHub 作品展示', status: 'PENDING' },
  { code: 'F5', title: '访客统计', status: 'PENDING' },
  { code: 'F6', title: '管理后台框架', status: 'ONLINE' },
  { code: 'F7', title: '简历下载', status: 'PENDING' },
  { code: 'F8', title: '照片墙 / 奖状栏', status: 'PENDING' },
  { code: 'F9', title: '工作经历', status: 'PENDING' },
  { code: 'F10', title: '命令面板', status: 'PENDING' },
  { code: 'F11', title: '控制台彩蛋', status: 'PENDING' },
  { code: 'F12', title: 'AI 聊天分身', status: 'PHASE-2' },
]
</script>

<template>
  <section class="admin-index">
    <div class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        SYSTEM ONLINE
      </h1>
      <p class="sub">$ admin --status · 检测到 13 个模块，2 个已上线</p>
    </div>

    <div class="grid">
      <article
        v-for="(m, i) in MODULES"
        :key="m.code"
        class="card"
        :class="{ online: m.status === 'ONLINE' }"
        :style="{ '--i': i }"
      >
        <div class="card-top">
          <span class="code">{{ m.code }}</span>
          <span class="badge" :class="m.status.toLowerCase()">{{ m.status }}</span>
        </div>
        <p class="name">{{ m.title }}</p>
      </article>
    </div>

    <p class="legend">
      ONLINE = 已上线 · PENDING = 待开发（按 Spec 顺序推进）· PHASE-2 = 二期
    </p>
  </section>
</template>

<style scoped>
.admin-index {
  display: flex;
  flex-direction: column;
  gap: 24px;
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

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
}

.card {
  padding: 14px 16px;
  background: var(--bg-panel);
  border: 2px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 10px;
  animation: rise-in 0.4s ease-out both;
  animation-delay: calc(var(--i) * 35ms);
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.15s;
}

.card:hover {
  border-color: var(--border-bright);
  transform: translateY(-2px);
}

.card.online {
  border-color: var(--green);
  box-shadow: 0 0 16px rgba(0, 255, 156, 0.1);
}

.card.online:hover {
  box-shadow: 0 0 22px rgba(0, 255, 156, 0.25);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code {
  font-family: var(--font-pixel);
  font-size: 11px;
  color: var(--cyan);
  text-shadow: 0 0 6px rgba(43, 217, 255, 0.3);
}

.card.online .code {
  color: var(--green);
  text-shadow: 0 0 6px var(--green-glow);
}

.badge {
  font-size: 10px;
  letter-spacing: 1px;
  padding: 2px 6px;
  border: 1px solid currentColor;
}

.badge.online {
  color: var(--green);
}

.badge.pending {
  color: var(--amber);
}

.badge.phase-2 {
  color: var(--text-dim);
}

.name {
  margin: 0;
  color: var(--text);
  font-size: 13px;
}

.legend {
  margin: 0;
  color: var(--text-dim);
  font-size: 12px;
}
</style>
