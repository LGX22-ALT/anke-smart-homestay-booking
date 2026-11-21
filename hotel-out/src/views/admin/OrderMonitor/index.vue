<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useOrderPage } from '@/composables/useOrderPage'
import { getAdminOrderPage } from '@/services/dataSource'
import { ElMessage } from 'element-plus'
import { computed } from 'vue'

import ManagementTable from '@/components/common/ManagementTable.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import { interveneAdminOrder } from '@/services/dataSource'

const { rows, total, pages, page, loading, error, load, changePage, search } =
  useOrderPage(getAdminOrderPage)
const columns = [
  { key: 'id', label: '订单号' },
  { key: 'hotel', label: '房源' },
  { key: 'guest', label: '住客' },
  { key: 'checkIn', label: '入住日期' },
  { key: 'amount', label: '交易金额' },
  { key: 'status', label: '订单状态' },
]
const validAmount = computed(() =>
  rows.value
    .filter((item) => item.paymentStatus === 'PAID')
    .reduce((sum, item) => sum + item.amount, 0),
)

async function completeRefund(row) {
  Object.assign(
    row,
    await interveneAdminOrder(row.id, { status: 'REFUNDED', remark: '管理员确认退款完成' }),
  )
  ElMessage.success('退款已完成')
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <p class="bo-kicker admin-kicker">TRANSACTION MONITORING</p>
        <h1>订单监控</h1>
        <p>跨商家查看交易状态与风险标记。</p>
      </div>
      <button class="bo-button" type="button" @click="load">刷新数据</button>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <section class="bo-grid monitor-grid">
      <MetricCard label="订单总数" :value="total" trend="当前筛选条件" tone="blue" /><MetricCard
        label="本页有效交易额"
        :value="`¥${validAmount.toLocaleString()}`"
        trend="本页已支付且未退款"
        tone="gold"
      /><MetricCard
        label="本页退款申请"
        :value="rows.filter((item) => item.orderStatus === 'REFUNDING').length"
        trend="等待人工复核"
        tone="red"
      />
    </section>
    <ManagementTable
      :rows="rows"
      :server="true"
      :total="total"
      :pages="pages"
      :current-page="page"
      :loading="loading"
      :columns="columns"
      :search-keys="['id', 'hotel', 'guest', 'status', 'risk']"
      placeholder="搜索订单号"
      @page-change="changePage"
      @search="search"
      ><template #cell-amount="{ value }">¥{{ value }}</template
      ><template #cell-risk="{ value }"
        ><span :class="['bo-tag', { 'bo-tag--danger': value !== '—' }]">{{ value }}</span></template
      ><template #actions="{ row }"
        ><button
          type="button"
          @click="$message.info(`${row.id} · ${row.phone} · ${row.createdAt}`)"
        >
          查看链路</button
        ><button v-if="row.orderStatus === 'REFUNDING'" type="button" @click="completeRefund(row)">
          确认退款
        </button></template
      ></ManagementTable
    >
  </div>
</template>

<style scoped>
.admin-kicker {
  color: #c53030;
}
.monitor-grid {
  grid-template-columns: repeat(3, 1fr);
}
@media (max-width: 700px) {
  .monitor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
