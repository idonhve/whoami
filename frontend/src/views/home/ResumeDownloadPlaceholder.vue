<script setup lang="ts">
/**
 * 「简历下载」占位区块：Spec 07（简历版本与下载）在此填充，无冲突接入。
 */
</script>

<template>
  <section class="home-slot" aria-label="简历下载（待上线）">
    <header class="slot-head">
      <span class="slot-cmd">$ wget ~/resume.pdf</span>
      <span class="slot-tag">SPEC-07 // PENDING</span>
    </header>
    <div class="slot-body">
      <p class="slot-line dim">简历下载模块接入中 —— 最新简历 PDF 将在此提供下载</p>
      <button type="button" class="neon-btn" disabled>DOWNLOAD &gt;</button>
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
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;
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
