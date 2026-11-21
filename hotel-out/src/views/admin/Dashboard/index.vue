<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import { onMounted, reactive } from 'vue'
import MetricCard from '@/components/common/MetricCard.vue'
import { getAdminOverview } from '@/services/dataSource'

const data = reactive({ users: 0, hotels: 0, orders: 0, todayAmount: 0 })
const { loading, error, load } = useLoadState(
  async () => {
    const result = await getAdminOverview()
    Object.assign(data, {
      users: Array.isArray(result.users) ? result.users.length : Number(result.users || 0),
      hotels: Array.isArray(result.hotels) ? result.hotels.length : Number(result.hotels || 0),
      orders: Array.isArray(result.orders) ? result.orders.length : Number(result.orders || 0),
      todayAmount: Number(result.todayAmount || 0),
    })
  },
  () => {
    Object.assign(data, { users: 0, hotels: 0, orders: 0, todayAmount: 0 })
  },
)
onMounted(load)
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <p class="bo-kicker admin-kicker">PLATFORM OPERATIONS</p>
        <h1>平台总览</h1>
        <p>审核、风控与平台运行状态集中监测。</p>
      </div>
      <RouterLink class="bo-button" to="/admin/audits">处理待审核</RouterLink>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <section v-if="!loading && !error" class="bo-grid">
      <MetricCard label="平台用户" :value="data.users" tone="blue" />
      <MetricCard label="房源总数" :value="data.hotels" />
      <MetricCard label="订单总数" :value="data.orders" tone="gold" />
      <MetricCard
        label="今日已支付交易额"
        :value="`¥${data.todayAmount.toLocaleString()}`"
        tone="red"
      />
    </section>
    <section class="bo-panel">
      <h2>管理入口</h2>
      <div class="todo-list">
        <RouterLink to="/admin/audits"
          ><div><strong>审核房源</strong><small>查看真实提交的房源资料</small></div></RouterLink
        >
        <RouterLink to="/admin/comments"
          ><div><strong>管理评价</strong><small>查看住客评价与展示状态</small></div></RouterLink
        >
        <RouterLink to="/admin/orders"
          ><div><strong>订单与退款</strong><small>查看订单状态并处理退款</small></div></RouterLink
        >
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-kicker {
  color: #c53030;
}
.admin-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 14px;
}
.category-list {
  display: grid;
  gap: 18px;
}
.category-list > div {
  display: grid;
  grid-template-columns: 90px 1fr 42px;
  align-items: center;
  gap: 12px;
  color: #52606d;
  font-size: 12px;
}
.category-list > div > div {
  height: 8px;
  background: #edf2f7;
}
.category-list i {
  display: block;
  height: 100%;
  background: #3e7cb1;
}
.category-list strong {
  text-align: right;
}
.todo-list {
  display: grid;
  gap: 9px;
}
.todo-list a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px;
  background: #f8fafc;
  color: #334e68;
  text-decoration: none;
}
.todo-list div {
  display: grid;
  gap: 3px;
}
.todo-list small {
  color: var(--muted);
}
.todo-number {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: var(--brand);
  background: #e9f5f3;
  font-weight: 700;
}
.todo-number.red {
  color: #b83232;
  background: #fcebea;
}
.todo-number.gold {
  color: #9b5c16;
  background: #fff4d6;
}
.activity {
  display: grid;
}
.activity p {
  display: grid;
  grid-template-columns: 70px 1fr;
  margin: 0;
  padding: 12px 0;
  border-top: 1px solid #e4e7eb;
  color: #52606d;
  font-size: 12px;
}
.activity time {
  color: var(--muted);
}
@media (max-width: 900px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
