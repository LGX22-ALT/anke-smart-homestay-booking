<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import { computed, onMounted, ref } from 'vue'
import MetricCard from '@/components/common/MetricCard.vue'
import { getMerchantOrders } from '@/services/dataSource'

const orders = ref([])
const { loading, error, load } = useLoadState(
  async () => {
    orders.value = await getMerchantOrders()
  },
  () => {
    orders.value = []
  },
)
onMounted(load)
const paidOrders = computed(() => orders.value.filter((order) => order.paymentStatus === 'PAID'))
const revenue = computed(() => paidOrders.value.reduce((sum, order) => sum + order.amount, 0))
const nightlyAverage = computed(() => {
  const nights = paidOrders.value.reduce((sum, order) => sum + Number(order.nights || 0), 0)
  return nights ? `¥${(revenue.value / nights).toFixed(2)}` : '—'
})
const months = computed(() => {
  const totals = new Map()
  for (const order of paidOrders.value) {
    const month = order.createdAt.slice(0, 7)
    if (month) totals.set(month, (totals.get(month) || 0) + order.amount)
  }
  const result = [...totals].sort(([a], [b]) => a.localeCompare(b)).slice(-6)
  const max = Math.max(1, ...result.map(([, value]) => value))
  return result.map(([label, value]) => ({ label, value, height: (value / max) * 100 }))
})
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <h1>数据统计</h1>
        <p>从收入、入住率和渠道结构理解经营表现。</p>
      </div>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <section v-if="!loading && !error" class="bo-grid">
      <MetricCard label="累计订单" :value="orders.length" trend="当前商家的全部订单" /><MetricCard
        label="累计已支付金额"
        :value="`¥${revenue.toLocaleString()}`"
        trend="不含退款订单"
        tone="gold"
      /><MetricCard
        label="已支付每晚均价"
        :value="nightlyAverage"
        trend="按订单晚数计算"
        tone="blue"
      /><MetricCard label="平均评分" value="—" trend="暂无评分汇总" />
    </section>
    <section class="stats-grid">
      <article class="bo-panel">
        <h2>月度收入趋势</h2>
        <p v-if="!months.length" class="bo-empty">暂无已支付收入记录</p>
        <div v-else class="revenue-chart">
          <div v-for="item in months" :key="item.label">
            <span :style="{ height: `${item.height}%` }"
              ><i>¥{{ item.value.toLocaleString() }}</i></span
            ><small>{{ item.label }}</small>
          </div>
        </div>
      </article>
      <article class="bo-panel">
        <h2>订单来源</h2>
        <p class="bo-empty">暂无渠道统计数据。</p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.period {
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  background: #fff;
}
.stats-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 14px;
}
.revenue-chart {
  display: flex;
  align-items: end;
  gap: 18px;
  height: 260px;
  padding-top: 35px;
}
.revenue-chart > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: end;
  height: 100%;
  gap: 8px;
}
.revenue-chart span {
  position: relative;
  width: min(42px, 70%);
  background: var(--brand);
}
.revenue-chart i {
  position: absolute;
  top: -22px;
  left: 50%;
  color: #52606d;
  font-size: 10px;
  font-style: normal;
  transform: translateX(-50%);
  white-space: nowrap;
}
.revenue-chart small {
  color: var(--muted);
}
.source-list {
  display: grid;
  gap: 19px;
}
.source-list > div > div {
  display: flex;
  justify-content: space-between;
  color: #52606d;
  font-size: 12px;
}
.source-list p {
  height: 7px;
  margin: 8px 0 0;
  background: #edf2f7;
}
.source-list i {
  display: block;
  height: 100%;
  background: #3e7cb1;
}
.insights > div {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.insights p {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 15px;
  border-left: 3px solid #d69e2e;
  background: #fffaf0;
}
.insights span {
  color: var(--muted);
  font-size: 11px;
  line-height: 1.7;
}
@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .insights > div {
    grid-template-columns: 1fr;
  }
}
</style>
