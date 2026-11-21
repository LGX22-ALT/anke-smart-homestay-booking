<script setup>
import { computed, ref, watch } from 'vue'
const props = defineProps({
  rows: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  searchKeys: { type: Array, default: () => [] },
  placeholder: { type: String, default: '搜索' },
  server: Boolean,
  total: { type: Number, default: 0 },
  pages: { type: Number, default: 0 },
  currentPage: { type: Number, default: 1 },
  loading: Boolean,
})
const emit = defineEmits(['action', 'page-change', 'search'])
const keyword = ref('')
const page = ref(1)
const pageSize = 8
const filtered = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return key
    ? props.rows.filter((row) =>
        props.searchKeys.some((field) =>
          String(row[field] ?? '')
            .toLowerCase()
            .includes(key),
        ),
      )
    : props.rows
})
const pageCount = computed(() =>
  Math.max(1, props.server ? props.pages : Math.ceil(filtered.value.length / pageSize)),
)
const activePage = computed(() => (props.server ? props.currentPage : page.value))
const visible = computed(() =>
  props.server
    ? props.rows
    : filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize),
)
watch(pageCount, (value) => {
  if (!props.server) page.value = Math.min(page.value, value)
})
function search() {
  page.value = 1
  if (props.server) emit('search', keyword.value.trim())
}
function changePage(value) {
  if (props.server) emit('page-change', value)
  else page.value = value
}
function action(name, row) {
  emit('action', { name, row })
}
</script>
<template>
  <section class="table-panel">
    <div class="table-tools">
      <div class="search-box">
        <span>⌕</span
        ><input
          v-model="keyword"
          :placeholder="placeholder"
          @input="!server && search()"
          @keyup.enter="search"
        />
        <button v-if="server" type="button" :disabled="loading" @click="search">搜索</button>
      </div>
      <slot name="tools" />
    </div>
    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th v-for="column in columns" :key="column.key">{{ column.label }}</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in visible" :key="row.id">
            <td v-for="column in columns" :key="column.key">
              <slot :name="`cell-${column.key}`" :row="row" :value="row[column.key]"
                ><span
                  v-if="column.key === 'status'"
                  :class="[
                    'bo-tag',
                    {
                      'bo-tag--warning': String(row[column.key]).includes('待'),
                      'bo-tag--danger': ['已取消', '已禁用', '待处理', '已下架'].includes(
                        row[column.key],
                      ),
                    },
                  ]"
                  >{{ row[column.key] }}</span
                ><template v-else>{{ row[column.key] }}</template></slot
              >
            </td>
            <td>
              <div class="row-actions">
                <slot name="actions" :row="row"
                  ><button type="button" @click="action('view', row)">查看</button></slot
                >
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !visible.length" class="bo-empty">暂无符合条件的数据</p>
    </div>
    <div class="pagination">
      <span>共 {{ server ? total : filtered.length }} 条</span>
      <div>
        <button
          type="button"
          :disabled="loading || activePage <= 1"
          @click="changePage(activePage - 1)"
        >
          上一页</button
        ><span>{{ activePage }} / {{ pageCount }}</span
        ><button
          type="button"
          :disabled="loading || activePage >= pageCount"
          @click="changePage(activePage + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </section>
</template>
<style scoped>
.table-panel {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fff;
}
.table-tools {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border-bottom: 1px solid #e4e7eb;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  width: min(100%, 320px);
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}
.search-box input {
  width: 100%;
  min-height: 36px;
  border: 0;
  outline: 0;
}
.table-scroll {
  overflow: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  white-space: nowrap;
}
th,
td {
  padding: 13px 14px;
  border-bottom: 1px solid #e4e7eb;
  text-align: left;
  font-size: 12px;
}
th {
  color: var(--muted);
  background: #f8fafc;
  font-weight: 600;
}
td {
  color: #334e68;
}
.row-actions {
  display: flex;
  gap: 7px;
}
.row-actions button {
  padding: 5px 8px;
  border: 1px solid #cbd5e1;
  border-radius: 3px;
  color: #52606d;
  background: #fff;
  cursor: pointer;
}
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 14px;
  color: var(--muted);
  font-size: 11px;
}
.pagination > div {
  display: flex;
  gap: 10px;
  align-items: center;
}
.pagination button {
  padding: 5px 9px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
}
.pagination button:disabled {
  opacity: 0.4;
}
</style>
