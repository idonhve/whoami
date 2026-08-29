<script setup lang="ts">
// M1 空壳：后续各模块（Spec 02~09）在 AdminLayout 下挂自己的管理页
</script>

<template>
  <section class="admin-index">
    <p class="prompt">$ admin --status</p>
    <p class="hint">管理后台骨架已就绪（M1 地基）。</p>
    <p class="hint">各内容管理模块将由 Spec 02~09 陆续挂载到本布局下。</p>
  </section>
</template>

<style scoped>
.admin-index {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prompt {
  color: var(--accent);
  margin: 0;
}

.hint {
  color: var(--text-dim);
  margin: 0;
}
</style>
