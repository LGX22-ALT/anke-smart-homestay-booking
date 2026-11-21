<script setup>
defineProps({ loading: Boolean, error: { type: String, default: '' } })
defineEmits(['retry'])
</script>
<template>
  <div v-if="loading" class="load-state" role="status" aria-live="polite">正在加载…</div>
  <div v-else-if="error" class="load-state load-state--error" role="alert">
    <div>
      <strong>加载失败</strong>
      <p>{{ error }}</p>
    </div>
    <button type="button" @click="$emit('retry')">重试</button>
  </div>
</template>
<style scoped>
.load-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 16px 0;
  padding: 18px;
  border: 1px solid var(--line);
  background: #f8fafc;
  color: #52606d;
}
.load-state--error {
  border-left: 3px solid #c53030;
  background: #fff8f7;
}
.load-state strong {
  color: #8e2828;
  font-size: 14px;
}
.load-state p {
  margin: 6px 0 0;
  font-size: 13px;
  overflow-wrap: anywhere;
}
.load-state button {
  min-height: 40px;
  flex-shrink: 0;
  padding: 0 16px;
  border: 1px solid var(--brand);
  background: #fff;
  color: var(--brand);
  cursor: pointer;
}
.load-state button:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 3px;
}
</style>
