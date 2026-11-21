<script setup>
import { computed } from 'vue'

defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: Boolean,
})

const emit = defineEmits(['update:modelValue'])
const stars = computed(() => Array.from({ length: 5 }, (_, index) => index + 1))
</script>

<template>
  <div class="rate-star" :aria-label="`${modelValue} 星`">
    <button
      v-for="star in stars"
      :key="star"
      type="button"
      :class="{ active: star <= modelValue }"
      :disabled="readonly"
      :aria-label="`${star} 星`"
      @click="emit('update:modelValue', star)"
    >
      ★
    </button>
  </div>
</template>

<style scoped>
.rate-star {
  display: inline-flex;
  gap: 2px;
}
.rate-star button {
  padding: 0;
  border: 0;
  color: #cbd5e1;
  background: none;
  cursor: pointer;
  font-size: 17px;
}
.rate-star button.active {
  color: #e6a23c;
}
.rate-star button:disabled {
  cursor: default;
}
</style>
