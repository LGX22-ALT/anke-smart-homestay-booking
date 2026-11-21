<script setup>
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import NavHeader from '@/components/common/NavHeader.vue'
import { createBooking, payBooking } from '@/services/dataSource'
import { useBookingStore, useUserStore } from '@/stores'
import { isValidMobile } from '@/utils/validate'

const router = useRouter()
const booking = useBookingStore()
const user = useUserStore()
const guest = reactive({ name: user.userInfo?.name || '', phone: user.userInfo?.phone || '' })
const submitting = ref(false)
const submitError = ref('')
const createdOrder = ref(null)
const failedImage = ref('')
const remark = ref('')
const isDevelopment = import.meta.env.DEV
const total = computed(
  () =>
    createdOrder.value?.amount ??
    booking.room?.stayTotal ??
    (booking.room?.price || 0) * booking.stayNights,
)

async function submit(status = 'success') {
  if (submitting.value) return
  if (!booking.hotel || !booking.room) return router.push('/search')
  if (!guest.name.trim() || !isValidMobile(guest.phone))
    return ElMessage.warning('请填写姓名和正确的手机号')
  submitting.value = true
  submitError.value = ''
  try {
    const quotedAmount = total.value
    if (!createdOrder.value)
      createdOrder.value = await createBooking({
        hotelId: booking.hotel.id,
        hotel: booking.hotel.name,
        roomTypeId: booking.room.id,
        guestName: guest.name.trim(),
        guestPhone: guest.phone,
        guest: guest.name.trim(),
        phone: guest.phone,
        checkIn: booking.checkInDate,
        checkOut: booking.checkOutDate,
        guests: booking.guestCount,
        remark: remark.value,
        requestId: booking.requestId,
      })
    if (Number(createdOrder.value.amount) !== Number(quotedAmount)) {
      submitError.value = `房价已更新为 ¥${createdOrder.value.amount}，请核对订单总额后再次支付`
      return
    }
    const order = await payBooking(createdOrder.value.id, status === 'success')
    await router.push({ name: 'pay-result', query: { orderId: order.id } })
  } catch (cause) {
    submitError.value = cause.message || '提交失败，请重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="order-page">
    <div class="page-header"><NavHeader /></div>
    <main class="order-main">
      <div>
        <h1>确认你的旅程</h1>
        <p class="lead">请核对房型、入住日期及联系人信息，订单进度可在“我的订单”中查看。</p>
      </div>
      <div class="order-layout">
        <section class="form-panel">
          <h2>入住人信息</h2>
          <label>姓名<input v-model="guest.name" placeholder="请输入真实姓名" /></label
          ><label>手机号<input v-model="guest.phone" placeholder="用于接收订单通知" /></label
          ><label
            >备注<textarea
              v-model="remark"
              maxlength="500"
              placeholder="如有特殊入住需求，请告诉房东"
            />
          </label>
          <h2>支付服务</h2>
          <div class="payment-options">
            <p>在线支付暂未开放。当前为沙箱支付验证，不产生实际扣款。</p>
          </div>
          <p v-if="submitError" role="alert">{{ submitError }}</p>
          <button
            class="submit-button"
            :disabled="submitting"
            type="button"
            @click="submit('success')"
          >
            提交订单并验证支付
          </button>
          <button
            v-if="isDevelopment"
            class="fail-button"
            :disabled="submitting"
            type="button"
            @click="submit('failed')"
          >
            开发调试：支付失败
          </button>
        </section>
        <aside v-if="booking.hotel" class="summary">
          <img
            v-if="booking.hotel.image && failedImage !== booking.hotel.image"
            :src="booking.hotel.image"
            :alt="booking.hotel.name"
            @error="failedImage = booking.hotel.image"
          />
          <div v-else class="image-placeholder">商家暂未上传图片</div>
          <h2>{{ booking.hotel.name }}</h2>
          <p>{{ booking.room?.name }}</p>
          <dl>
            <div>
              <dt>入住</dt>
              <dd>{{ booking.checkInDate }}</dd>
            </div>
            <div>
              <dt>离店</dt>
              <dd>{{ booking.checkOutDate }}</dd>
            </div>
            <div>
              <dt>晚数</dt>
              <dd>{{ booking.stayNights }} 晚</dd>
            </div>
            <div>
              <dt>住客</dt>
              <dd>{{ booking.guestCount }} 位</dd>
            </div>
          </dl>
          <div class="total">
            <span>订单总额</span><strong>¥{{ total }}</strong>
          </div>
        </aside>
        <aside v-else class="summary empty">
          <h2>尚未选择房源</h2>
          <RouterLink to="/search">去搜索房源</RouterLink>
        </aside>
      </div>
    </main>
  </div>
</template>

<style scoped>
.order-page {
  min-height: 100vh;
  color: var(--ink);
  background: var(--canvas);
}
.page-header {
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.order-main {
  width: min(100% - 48px, 1080px);
  margin: 0 auto;
  padding: 70px 0 110px;
}
.kicker {
  margin: 0 0 12px;
  color: var(--brand);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
}
h1 {
  margin: 0;
  font-family: var(--font-ui);
  font-size: clamp(40px, 6vw, 64px);
  font-weight: 400;
}
.lead {
  color: var(--muted);
}
.order-layout {
  display: grid;
  grid-template-columns: 1fr 390px;
  gap: 26px;
  align-items: start;
  margin-top: 45px;
}
.form-panel,
.summary {
  padding: 28px;
  border: 1px solid var(--line);
  background: #fff;
}
.form-panel {
  display: grid;
  gap: 18px;
}
.form-panel h2 {
  margin: 8px 0 0;
  font-size: 20px;
}
.form-panel label:not(.pay-option) {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}
.form-panel input,
.form-panel textarea {
  min-height: 44px;
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}
.form-panel textarea {
  min-height: 90px;
  resize: vertical;
}
.pay-option {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 14px;
  background: var(--brand-soft);
}
.pay-option input {
  min-height: auto;
}
.payment-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.submit-button {
  min-height: 50px;
  border: 0;
  color: #fff;
  background: var(--brand);
  cursor: pointer;
  font-weight: 700;
}
.fail-button {
  min-height: 42px;
  border: 1px solid #cbd5e1;
  color: var(--muted);
  background: #fff;
  cursor: pointer;
}
.summary {
  position: sticky;
  top: 24px;
}
.summary img,
.image-placeholder {
  width: 100%;
  aspect-ratio: 1.8;
  object-fit: cover;
}
.image-placeholder {
  display: grid;
  place-items: center;
  color: var(--muted);
  background: var(--brand-soft);
  font-size: 13px;
}
.summary h2 {
  margin: 18px 0 6px;
}
.summary > p {
  color: var(--muted);
}
.summary dl {
  display: grid;
  gap: 10px;
  margin: 22px 0;
}
.summary dl div {
  display: flex;
  justify-content: space-between;
}
.summary dt {
  color: var(--muted);
}
.summary dd {
  margin: 0;
}
.total {
  display: flex;
  justify-content: space-between;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}
.total strong {
  font-size: 25px;
}
.summary.empty {
  position: static;
}
.summary.empty a {
  color: var(--brand);
}
@media (max-width: 800px) {
  .order-layout {
    grid-template-columns: 1fr;
  }
  .summary {
    position: static;
    order: -1;
  }
}
@media (max-width: 560px) {
  .order-main {
    width: min(100% - 32px, 1080px);
    padding-top: 48px;
  }
}
</style>
