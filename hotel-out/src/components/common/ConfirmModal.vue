<script setup>
defineProps({
  modelValue: Boolean,
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定继续执行此操作吗？' },
  confirmText: { type: String, default: '确认' },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

function close() {
  emit('update:modelValue', false)
}

function confirm() {
  emit('confirm')
  close()
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="modal-backdrop" role="presentation" @click.self="close">
      <section
        class="modal-panel"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="`${title}-title`"
      >
        <h2 :id="`${title}-title`">{{ title }}</h2>
        <p>{{ message }}</p>
        <div class="modal-actions">
          <button type="button" class="button button--ghost" @click="close">取消</button>
          <button type="button" class="button button--primary" @click="confirm">
            {{ confirmText }}
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgb(16 42 67 / 55%);
}

.modal-panel {
  width: min(100%, 420px);
  padding: 28px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 80px rgb(0 0 0 / 20%);
}

h2 {
  margin: 0;
  color: var(--ink);
  font-size: 21px;
}

p {
  margin: 14px 0 26px;
  color: var(--muted);
  line-height: 1.7;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.button {
  min-height: 40px;
  padding: 0 18px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  cursor: pointer;
}

.button--ghost {
  color: #52606d;
  background: #fff;
}

.button--primary {
  border-color: var(--brand);
  color: #fff;
  background: var(--brand);
}
</style>
