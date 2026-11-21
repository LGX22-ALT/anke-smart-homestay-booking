<script setup>
defineProps({
  label: String,
  value: [String, Number],
  trend: String,
  tone: { type: String, default: 'teal' },
})
</script>
<template>
  <article :class="['metric-card', `metric-card--${tone}`]">
    <span>{{ label }}</span
    ><strong>{{ value }}</strong
    ><small>{{ trend }}</small>
  </article>
</template>
<style scoped>
.metric-card {
  display: grid;
  gap: 9px;
  padding: 20px;
  border: 1px solid var(--line);
  border-top: 3px solid var(--brand);
  border-radius: 6px;
  background: #fff;
}
.metric-card--gold {
  border-top-color: #e6a23c;
}
.metric-card--red {
  border-top-color: #d64545;
}
.metric-card--blue {
  border-top-color: #3e7cb1;
}
.metric-card span {
  color: var(--muted);
  font-size: 12px;
}
.metric-card strong {
  color: var(--ink);
  font-size: 28px;
}
.metric-card small {
  color: #2f9e83;
}
</style>
