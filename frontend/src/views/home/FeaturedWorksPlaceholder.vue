<script setup lang="ts">
/**
 * 「精选作品」占位区块：Spec 04（GitHub 作品展示）在此填充，无冲突接入。
 */
</script>

<template>
  <section class="home-slot" aria-label="精选作品（待上线）">
    <header class="slot-head">
      <span class="slot-cmd">$ ls ~/featured</span>
      <span class="slot-tag">SPEC-04 // PENDING</span>
    </header>
    <div class="slot-body">
      <p class="slot-line dim">精选作品模块接入中 —— GitHub 同步数据将在此展示</p>
    </div>
  </section>
</template>

<style scoped>
.home-slot {
  width: min(960px, 92vw);
  margin: 0 auto 64px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
}

.slot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border);
}

.slot-cmd {
  font-family: var(--font-term);
  font-size: 18px;
  color: var(--green);
  text-shadow: 0 0 6px var(--green-glow);
}

.slot-tag {
  font-family: var(--font-pixel);
  font-size: 8px;
  letter-spacing: 1px;
  color: var(--amber);
}

.slot-body {
  padding: 28px 20px;
}

.slot-line {
  margin: 0;
  font-size: 14px;
}

.slot-line.dim {
  color: var(--text-dim);
}
</style>
