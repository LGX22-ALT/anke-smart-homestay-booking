<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import { computed, onMounted, reactive } from 'vue'

import MetricCard from '@/components/common/MetricCard.vue'
import { getMerchantOrders, getMerchantOverview, getMerchantReviews } from '@/services/dataSource'

const data = reactive({ orders: [], reviews: [] })
const { loading, error, load } = useLoadState(
  async () => {
    const [overview, orders, reviews] = await Promise.all([
      getMerchantOverview(),
      getMerchantOrders(),
      getMerchantReviews(),
    ])
    Object.assign(data, overview, { orders, reviews })
  },
  () => {
    data.orders = []
    data.reviews = []
  },
)
onMounted(load)
const todayOrders = computed(
  () =>
    data.orders.filter(
      (item) => item.createdAt.slice(0, 10) === new Date().toLocaleDateString('en-CA'),
    ).length,
)
const revenue = computed(() =>
  data.orders
    .filter((item) => item.paymentStatus === 'PAID')
    .reduce((sum, item) => sum + item.amount, 0),
)
const recentOrders = computed(() => data.orders.slice(0, 5))
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <h1>经营总览</h1>
        <p>把今天需要处理的订单与房态放在最前面。</p>
      </div>
      <RouterLink class="bo-button bo-button--primary" to="/merchant/hotels">+ 发布房源</RouterLink>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <section v-if="!loading && !error" class="bo-grid">
      <MetricCard label="今日新增订单" :value="todayOrders" trend="按订单创建日期统计" />
      <MetricCard
        label="累计已支付金额"
        :value="`¥${revenue.toLocaleString()}`"
        trend="不含退款订单"
        tone="gold"
      />
      <MetricCard label="平均入住率" value="—" trend="暂无入住率统计" tone="blue" />
      <MetricCard
        label="待回复评价"
        :value="data.reviews.filter((item) => !item.reply).length"
        trend="建议 24 小时内回复"
        tone="red"
      />
    </section>
    <section class="dashboard-grid">
      <article class="bo-panel chart-panel">
        <h2>入住率趋势</h2>
        <p class="bo-empty">入住率统计暂未接入，请在房态日历查看可售库存。</p>
      </article>
      <article class="bo-panel">
        <h2>快捷工作台</h2>
        <div class="quick-links">
          <RouterLink to="/merchant/calendar"
            ><strong>房态与价格</strong><span>批量调整未来可售库存</span></RouterLink
          ><RouterLink to="/merchant/orders"
            ><strong>待处理订单</strong><span>确认入住与取消申请</span></RouterLink
          ><RouterLink to="/merchant/comments"
            ><strong>住客评价</strong><span>及时回应住客反馈</span></RouterLink
          >
        </div>
      </article>
    </section>
    <section class="bo-panel">
      <div class="panel-title">
        <h2>最新订单</h2>
        <RouterLink to="/merchant/orders">查看全部</RouterLink>
      </div>
      <div class="mini-table">
        <div class="mini-row mini-row--head">
          <span>订单号</span><span>住客</span><span>入住日期</span><span>金额</span
          ><span>状态</span>
        </div>
        <div v-for="order in recentOrders" :key="order.id" class="mini-row">
          <span>{{ order.id }}</span
          ><span>{{ order.guest }}</span
          ><span>{{ order.checkIn }}</span
          ><span>¥{{ order.amount }}</span
          ><span
            ><i class="bo-tag">{{ order.status }}</i></span
          >
        </div>
      </div>
      <p v-if="!loading && !error && !recentOrders.length" class="bo-empty">暂无订单</p>
    </section>
  </div>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 14px;
}
.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.panel-title p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 11px;
}
.panel-title strong {
  color: var(--brand);
  font-size: 24px;
}
.panel-title a {
  color: var(--brand);
  font-size: 12px;
  text-decoration: none;
}
.bar-chart {
  display: flex;
  align-items: end;
  gap: 16px;
  height: 210px;
  padding-top: 22px;
}
.bar-column {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: end;
  height: 100%;
  gap: 8px;
}
.bar-column span {
  width: min(34px, 70%);
  min-height: 6px;
  background: var(--brand);
}
.bar-column small {
  color: var(--muted);
}
.quick-links {
  display: grid;
  gap: 8px;
}
.quick-links a {
  display: grid;
  gap: 4px;
  padding: 13px;
  border-left: 3px solid var(--brand);
  color: #334e68;
  background: #f8fafc;
  text-decoration: none;
}
.quick-links span {
  color: var(--muted);
  font-size: 11px;
}
.mini-table {
  overflow: auto;
}
.mini-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 0.7fr 0.8fr;
  min-width: 650px;
  padding: 12px;
  border-top: 1px solid #e4e7eb;
  color: #52606d;
  font-size: 12px;
}
.mini-row--head {
  color: var(--muted);
  background: #f8fafc;
}
@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
