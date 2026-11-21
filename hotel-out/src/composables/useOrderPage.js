import { onMounted, ref } from 'vue'
import { useLoadState } from './useLoadState'

export function useOrderPage(fetchPage, autoLoad = true) {
  const rows = ref([])
  const total = ref(0)
  const pages = ref(0)
  const page = ref(1)
  const keyword = ref('')
  const status = ref('')
  const state = useLoadState(
    async (isCurrent) => {
      const result = await fetchPage({
        pageNum: page.value,
        pageSize: 10,
        keyword: keyword.value || undefined,
        status: status.value || undefined,
      })
      if (!isCurrent()) return
      rows.value = result.list
      total.value = result.total
      pages.value = result.pages
    },
    () => {
      rows.value = []
      total.value = 0
      pages.value = 0
    },
  )
  function changePage(value) {
    page.value = value
    return state.load()
  }
  function search(value) {
    keyword.value = value
    return changePage(1)
  }
  function filter(value) {
    status.value = value
    return changePage(1)
  }
  if (autoLoad) onMounted(state.load)
  return { rows, total, pages, page, ...state, changePage, search, filter }
}
