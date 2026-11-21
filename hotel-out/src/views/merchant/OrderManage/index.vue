<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useOrderPage } from '@/composables/useOrderPage'
import { getMerchantOrderPage } from '@/services/dataSource'
import { ElMessage, ElMessageBox } from 'element-plus'

import ManagementTable from '@/components/common/ManagementTable.vue'
import { transitionMerchantOrder } from '@/services/dataSource'

const { rows, total, pages, page, loading, error, load, changePage, search } =
  useOrderPage(getMerchantOrderPage)
const columns = [
  { key: 'id', label: '订单号' },
  { key: 'guest', label: '住客' },
  { key: 'phone', label: '联系电话' },
  { key: 'checkIn', label: '入住' },
  { key: 'checkOut', label: '离店' },
  { key: 'amount', label: '金额' },
  { key: 'status', label: '状态' },
]

async function changeStatusApi(row, action) {
  if (action === 'cancel')
    await ElMessageBox.confirm(`确认取消订单 ${row.orderNo || row.id}？`, '取消订单', {
      type: 'warning',
    })
  Object.assign(row, await transitionMerchantOrder(row.id, action))
  ElMessage.success('订单状态已更新')
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <p class="bo-kicker">RESERVATION OPERATIONS</p>
        <h1>订单管理</h1>
        <p>确认预订、办理入住并跟踪订单状态。</p>
      </div>
      <button class="bo-button" type="button" disabled>导出报表暂未开放</button>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <ManagementTable
      :rows="rows"
      :server="true"
      :total="total"
      :pages="pages"
      :current-page="page"
      :loading="loading"
      :columns="columns"
      :search-keys="['id', 'guest', 'phone', 'status']"
      placeholder="搜索订单号"
      @page-change="changePage"
      @search="search"
      ><template #cell-amount="{ value }">¥{{ value }}</template
      ><template #actions="{ row }"
        ><button
          v-if="row.status === '待确认'"
          type="button"
          @click="changeStatusApi(row, 'confirm')"
        >
          确认</button
        ><button
          v-if="row.status === '待入住'"
          type="button"
          @click="changeStatusApi(row, 'checkIn')"
        >
          办理入住</button
        ><button
          v-if="['待支付', '待确认', '待入住'].includes(row.status)"
          type="button"
          @click="changeStatusApi(row, 'cancel')"
        >
          取消</button
        ><button v-else type="button" @click="$message.info(`订单 ${row.id} · ${row.hotel}`)">
          详情
        </button></template
      ></ManagementTable
    >
  </div>
</template>
