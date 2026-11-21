<script setup>
import { computed, ref, watch } from 'vue'
import { DEFAULT_AVATARS } from '@/constants/avatars'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '住客' },
  label: { type: String, default: '' },
})
const failed = ref(false)
const source = computed(() => props.src || DEFAULT_AVATARS[0].url)
watch(source, () => (failed.value = false))
</script>

<template>
  <span class="account-avatar">
    <img v-if="!failed" :src="source" :alt="label || `${name}的头像`" @error="failed = true" />
    <span v-else role="img" :aria-label="label || `${name}的头像`">{{ name.slice(0, 1) }}</span>
  </span>
</template>

<style scoped>
.account-avatar {
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  vertical-align: middle;
}
.account-avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
