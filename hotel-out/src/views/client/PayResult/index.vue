<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import NavHeader from '@/components/common/NavHeader.vue'
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import { getBooking } from '@/services/dataSource'
const route = useRoute()
const order = ref(null)
const { loading, error, load } = useLoadState(
  async (isCurrent) => {
    if (!route.query.orderId) throw new Error('缺少订单编号，请在我的订单中查看')
    const result = await getBooking(route.query.orderId)
    if (!result) throw new Error('订单不存在')
    if (isCurrent()) order.value = result
  },
  () => {
    order.value = null
  },
)
const success = computed(
  () =>
    order.value?.paymentStatus === 'PAID' ||
    ['待确认', '待入住', '已入住', '已完成'].includes(order.value?.status),
)
const title = computed(() =>
  success.value
    ? '预订成功'
    : ['已取消', '已退款', '退款中'].includes(order.value?.status)
      ? order.value.status
      : '支付未完成',
)
onMounted(load)
watch(() => route.query.orderId, load)
</script>

<template>
  <div class="result-page">
    <div class="page-header"><NavHeader /></div>
    <main>
      <LoadState :loading="loading" :error="error" @retry="load" />
      <template v-if="order && !loading && !error">
        <span :class="['result-icon', { failed: !success }]">{{ success ? '✓' : '×' }}</span>
        <h1>{{ title }}</h1>
        <p>
          {{
            success
              ? '房东已收到你的预订，行程详情可以在个人中心查看。'
              : `当前订单状态：${order.status}，请在我的订单中查看详情。`
          }}
        </p>
        <code>{{ order.orderNo || order.id }}</code>
      </template>
      <div class="actions">
        <RouterLink to="/user/orders">查看我的订单</RouterLink
        ><RouterLink class="secondary" to="/">返回首页</RouterLink>
      </div>
    </main>
  </div>
</template>

<style scoped>
.result-page {
  min-height: 100vh;
  color: var(--ink);
  background: var(--canvas);
}
.page-header {
  border-bottom: 1px solid var(--line);
  background: #fff;
}
main {
  display: grid;
  justify-items: center;
  width: min(100% - 40px, 680px);
  margin: 0 auto;
  padding: 110px 0;
  text-align: center;
}
.result-icon {
  display: grid;
  width: 82px;
  height: 82px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #2f9e83;
  font-size: 38px;
}
.result-icon.failed {
  background: #d64545;
}
.kicker {
  margin: 25px 0 10px;
  color: var(--brand);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
}
h1 {
  margin: 0;
  font-family: var(--font-ui);
  font-size: clamp(42px, 7vw, 68px);
  font-weight: 400;
}
main > p:not(.kicker) {
  max-width: 34em;
  color: var(--muted);
  line-height: 1.8;
}
code {
  margin-top: 14px;
  padding: 8px 12px;
  background: #e9f5f3;
}
.actions {
  display: flex;
  gap: 10px;
  margin-top: 34px;
}
.actions a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding: 0 20px;
  color: #fff;
  background: var(--brand);
  text-decoration: none;
}
.actions a.secondary {
  color: var(--ink);
  background: #fff;
  border: 1px solid #cbd5e1;
}
</style>
