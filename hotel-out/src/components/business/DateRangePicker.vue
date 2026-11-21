<script setup>
const props = defineProps({
  checkIn: { type: String, default: '' },
  checkOut: { type: String, default: '' },
  minDate: { type: String, default: '' },
})

const emit = defineEmits(['update:checkIn', 'update:checkOut'])
</script>

<template>
  <div class="date-range">
    <label>
      <span>入住日期</span>
      <input
        :value="props.checkIn"
        type="date"
        :min="minDate"
        @input="emit('update:checkIn', $event.target.value)"
      />
    </label>
    <span class="date-arrow" aria-hidden="true">→</span>
    <label>
      <span>离店日期</span>
      <input
        :value="props.checkOut"
        type="date"
        :min="props.checkIn || minDate"
        @input="emit('update:checkOut', $event.target.value)"
      />
    </label>
  </div>
</template>

<style scoped>
.date-range {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 12px;
  align-items: end;
}
.date-range label {
  display: grid;
  gap: 7px;
}
.date-range label span {
  color: var(--muted);
  font-size: 12px;
}
.date-range input {
  width: 100%;
  min-height: 42px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  color: #243b53;
  background: #fff;
}
.date-arrow {
  padding-bottom: 12px;
  color: var(--muted);
}
@media (max-width: 480px) {
  .date-range {
    grid-template-columns: 1fr;
  }
  .date-arrow {
    display: none;
  }
}
</style>
