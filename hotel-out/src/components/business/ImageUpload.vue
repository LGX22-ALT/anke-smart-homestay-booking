<script setup>
import { ref } from 'vue'

defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const input = ref(null)

function selectFiles(event) {
  const urls = [...event.target.files].map((file) => URL.createObjectURL(file))
  emit('update:modelValue', urls)
}
</script>

<template>
  <div class="image-upload">
    <button type="button" @click="input?.click()">＋ 上传图片</button>
    <input ref="input" type="file" accept="image/*" multiple hidden @change="selectFiles" />
    <div v-if="modelValue.length" class="image-preview">
      <img v-for="image in modelValue" :key="image" :src="image" alt="房源预览" />
    </div>
  </div>
</template>

<style scoped>
.image-upload button {
  min-height: 42px;
  padding: 0 18px;
  border: 1px dashed var(--muted);
  border-radius: 4px;
  color: var(--brand);
  background: #f0f8f7;
  cursor: pointer;
}
.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.image-preview img {
  width: 92px;
  aspect-ratio: 1.25;
  object-fit: cover;
  border-radius: 4px;
}
</style>
