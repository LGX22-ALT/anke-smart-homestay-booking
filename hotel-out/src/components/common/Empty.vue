<script setup>
defineProps({
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '当前没有符合条件的数据。' },
})
</script>

<template>
  <div class="empty-state">
    <span class="empty-mark">H</span>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty-state {
  display: grid;
  justify-items: center;
  padding: 64px 20px;
  color: var(--muted);
  text-align: center;
}

.empty-mark {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  margin-bottom: 18px;
  border: 1px solid #9fb3c8;
  border-radius: 50%;
  color: var(--brand);
  font-family: var(--font-ui);
  font-size: 24px;
}

strong {
  color: #243b53;
  font-size: 17px;
}

p {
  margin: 8px 0 0;
  font-size: 13px;
}
</style>
