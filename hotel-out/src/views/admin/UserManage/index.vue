<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'

import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'

import ManagementTable from '@/components/common/ManagementTable.vue'
import { getAdminUsers, setAdminUserStatus } from '@/services/dataSource'

const rows = ref([])
const columns = [
  { key: 'id', label: '用户 ID' },
  { key: 'name', label: '名称' },
  { key: 'phone', label: '手机号' },
  { key: 'role', label: '身份' },
  { key: 'orders', label: '订单数' },
  { key: 'registeredAt', label: '注册时间' },
  { key: 'status', label: '状态' },
]
const roleLabel = { user: '住客', merchant: '商家', admin: '管理员' }
const { loading, error, load } = useLoadState(
  async () => {
    rows.value = await getAdminUsers({ pageSize: 100 })
  },
  () => {
    rows.value = []
  },
)
onMounted(load)

async function toggleApi(row) {
  const enabled = row.status !== '正常'
  await ElMessageBox.confirm(`确认将 ${row.name} 设为${enabled ? '正常' : '禁用'}？`, '用户状态', {
    type: 'warning',
  })
  await setAdminUserStatus(row.id, enabled)
  row.status = enabled ? '正常' : '已禁用'
  ElMessage.success('用户状态已更新')
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <p class="bo-kicker admin-kicker">ACCOUNT GOVERNANCE</p>
        <h1>用户管理</h1>
        <p>管理住客与商家账号，处置异常访问。</p>
      </div>
      <button class="bo-button" type="button" disabled>导出用户暂未开放</button>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <ManagementTable
      :rows="rows"
      :columns="columns"
      :search-keys="['name', 'phone', 'role', 'status']"
      placeholder="搜索名称、手机号或身份"
      ><template #cell-role="{ value }">{{ roleLabel[value] }}</template
      ><template #actions="{ row }"
        ><button type="button" @click="$message.info(`${row.name} 共完成 ${row.orders} 笔订单`)">
          详情</button
        ><button type="button" @click="toggleApi(row)">
          {{ row.status === '正常' ? '禁用' : '启用' }}
        </button></template
      ></ManagementTable
    >
  </div>
</template>

<style scoped>
.admin-kicker {
  color: #c53030;
}
</style>
