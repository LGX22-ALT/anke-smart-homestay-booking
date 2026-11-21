import { ref } from 'vue'

export function useLoadState(loader, reset = () => {}) {
  const loading = ref(false)
  const error = ref('')
  let version = 0
  async function load() {
    const current = ++version
    loading.value = true
    error.value = ''
    reset()
    try {
      await loader(() => current === version)
    } catch (cause) {
      if (current === version) {
        reset()
        error.value = cause.message || '暂时无法获取数据，请重试'
      }
    } finally {
      if (current === version) loading.value = false
    }
  }
  return { loading, error, load }
}
