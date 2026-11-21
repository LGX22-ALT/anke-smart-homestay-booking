<script setup>
import {
  Calendar,
  ChatDotRound,
  CopyDocument,
  Location,
  Phone,
  Ticket,
  User,
} from '@element-plus/icons-vue'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

import { toOrderDisplay } from '@/utils/orderDisplay'

const props = defineProps({
  order: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['cancel', 'refund', 'contact'])
const display = computed(() => toOrderDisplay(props.order, props.order.hotelDetails))
const canCancel = computed(() =>
  ['PENDING_PAYMENT', 'PAID', 'PENDING_CHECKIN', 'CONFIRMED'].includes(display.value.orderStatus),
)
const canRefund = computed(() =>
  ['PAID', 'PENDING_CHECKIN', 'CONFIRMED', 'COMPLETED'].includes(display.value.orderStatus),
)
const hasHotel = computed(() => Boolean(display.value.hotelId))

function formatAmount(value) {
  return Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function maskPhone(value) {
  if (!value || value === '联系方式待确认') return value
  return String(value).replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

function copy(value, label) {
  if (!value || value === '待生成' || value === '地址信息待确认') return
  const fallback = () => {
    const input = document.createElement('textarea')
    input.value = value
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    input.remove()
  }
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(String(value)).catch(fallback)
  } else {
    fallback()
  }
  ElMessage.success(`${label}已复制`)
}

function openMap() {
  const address = display.value.hotelAddress
  if (!address || address === '地址信息待确认') {
    ElMessage.info('暂未提供酒店地址')
    return
  }
  window.open(
    `https://uri.amap.com/search?keyword=${encodeURIComponent(address)}`,
    '_blank',
    'noopener,noreferrer',
  )
}

function contactMerchant() {
  const phone = props.order.merchantPhone || props.order.contactPhone
  if (phone) {
    window.location.href = `tel:${phone}`
    return
  }
  emit('contact', display.value)
}
</script>

<template>
  <article class="order-detail-card">
    <header class="order-card-header">
      <div class="order-meta">
        <span class="order-label">订单号</span>
        <strong>{{ display.orderNo }}</strong>
        <button class="copy-button" type="button" @click="copy(display.orderNo, '订单号')">
          <CopyDocument />
          复制
        </button>
      </div>
      <div class="order-status">
        <span :class="['status-dot', `status-${display.orderStatus || 'default'}`]"></span>
        {{ display.statusText }}
      </div>
    </header>

    <section class="hotel-summary">
      <RouterLink
        v-if="hasHotel"
        class="hotel-cover"
        :to="{ name: 'hotel-detail', params: { id: display.hotelId } }"
      >
        <img
          v-if="display.hotelImage"
          :src="display.hotelImage"
          :alt="display.hotelName"
          loading="lazy"
          @error="(event) => (event.target.style.display = 'none')"
        />
        <span v-else>HOTEL</span>
      </RouterLink>
      <div v-else class="hotel-cover hotel-cover-empty">HOTEL</div>
      <div class="hotel-info">
        <RouterLink
          v-if="hasHotel"
          class="hotel-name"
          :to="{ name: 'hotel-detail', params: { id: display.hotelId } }"
        >
          {{ display.hotelName }}
        </RouterLink>
        <h2 v-else class="hotel-name">{{ display.hotelName }}</h2>
        <p class="hotel-address"><Location /> {{ display.hotelAddress }}</p>
        <div class="hotel-actions">
          <button type="button" @click="openMap"><Location /> 查看地图</button>
          <RouterLink v-if="hasHotel" :to="`/messages/${display.hotelId}`">
            <Phone /> 联系商家
          </RouterLink>
          <button v-else type="button" @click="contactMerchant"><Phone /> 联系商家</button>
          <RouterLink
            v-if="hasHotel"
            :to="{ name: 'hotel-detail', params: { id: display.hotelId } }"
          >
            <ChatDotRound /> 查看房源
          </RouterLink>
        </div>
      </div>
      <div class="order-total">
        <span>订单总额</span>
        <strong>¥{{ formatAmount(display.amount) }}</strong>
      </div>
    </section>

    <section class="room-detail">
      <div class="room-heading">
        <div>
          <span class="eyebrow">已预订房型</span>
          <h3>{{ display.roomName }}</h3>
        </div>
        <span v-if="display.nights > 0" class="nights">{{ display.nights }}晚</span>
      </div>
      <div class="room-facts">
        <span><Ticket /> {{ display.roomBed }}</span>
        <span v-if="display.roomArea">{{ display.roomArea }}㎡</span>
        <span v-if="display.roomGuests">{{ display.roomGuests }}人入住</span>
        <span v-for="facility in display.roomFacilities.slice(0, 4)" :key="facility">
          {{ facility }}
        </span>
      </div>
    </section>

    <section class="stay-timeline">
      <div class="stay-point">
        <span class="point-mark">住</span>
        <div>
          <span>入住</span>
          <strong
            >{{ display.checkIn }} <small>{{ display.checkInTime }}后</small></strong
          >
        </div>
      </div>
      <div class="timeline-line"></div>
      <div class="stay-point">
        <span class="point-mark">离</span>
        <div>
          <span>离店</span>
          <strong
            >{{ display.checkOut }} <small>{{ display.checkOutTime }}前</small></strong
          >
        </div>
      </div>
    </section>

    <section class="order-information">
      <div>
        <span><User /> 入住人</span>
        <strong>{{ display.guestName }}</strong>
        <small>{{ maskPhone(display.guestPhone) }}</small>
      </div>
      <div>
        <span><Calendar /> 下单时间</span>
        <strong>{{ display.createdAt }}</strong>
      </div>
    </section>

    <footer class="order-card-footer">
      <span class="footer-hint">请按入住信息准时到店，详情可联系商家确认</span>
      <div class="footer-actions">
        <button
          v-if="canCancel"
          class="button-quiet"
          type="button"
          @click="emit('cancel', props.order)"
        >
          取消订单
        </button>
        <button
          v-if="canRefund"
          class="button-quiet"
          type="button"
          @click="emit('refund', props.order)"
        >
          申请退款
        </button>
        <RouterLink
          v-if="hasHotel"
          class="button-primary"
          :to="{ name: 'hotel-detail', params: { id: display.hotelId } }"
        >
          再次预订
        </RouterLink>
      </div>
    </footer>
  </article>
</template>

<style scoped>
.order-detail-card {
  overflow: hidden;
  border: 1px solid #dfe6ed;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 12px 30px rgb(19 49 72 / 7%);
}
.order-card-header,
.hotel-summary,
.room-detail,
.stay-timeline,
.order-information,
.order-card-footer {
  padding: 18px 22px;
}
.order-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #edf1f5;
  background: #fbfcfd;
}
.order-meta,
.order-status,
.hotel-actions,
.room-facts,
.footer-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.order-meta {
  color: #536577;
  font-size: 12px;
}
.order-meta strong {
  color: #20384f;
  font-size: 13px;
}
.order-label,
.order-total span,
.eyebrow,
.stay-point span,
.order-information span,
.footer-hint {
  color: #8291a1;
  font-size: 12px;
}
.copy-button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  border: 0;
  color: #168b8b;
  background: none;
  cursor: pointer;
  font-size: 12px;
}
.copy-button svg {
  width: 13px;
}
.order-status {
  color: #168b8b;
  font-size: 13px;
  font-weight: 700;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #168b8b;
}
.status-PENDING_PAYMENT {
  background: #e08a2e;
}
.status-CANCELLED,
.status-REFUNDED {
  background: #94a3b8;
}
.status-REFUNDING {
  background: #6e56b4;
}
.hotel-summary {
  display: grid;
  grid-template-columns: 124px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
}
.hotel-cover {
  display: grid;
  width: 124px;
  height: 94px;
  place-items: center;
  overflow: hidden;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #2b6571, #173a51);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-decoration: none;
}
.hotel-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hotel-cover-empty {
  opacity: 0.8;
}
.hotel-info {
  min-width: 0;
}
.hotel-name {
  display: block;
  overflow: hidden;
  margin: 0 0 8px;
  color: #17324a;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.35;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hotel-address {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  margin: 0 0 13px;
  color: #738398;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hotel-address svg {
  width: 14px;
  color: #168b8b;
}
.hotel-actions button,
.hotel-actions a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 9px;
  border: 1px solid #dbe3ea;
  border-radius: 5px;
  color: #536577;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  text-decoration: none;
}
.hotel-actions button:hover,
.hotel-actions a:hover {
  border-color: #168b8b;
  color: #168b8b;
}
.hotel-actions svg {
  width: 13px;
}
.order-total {
  align-self: start;
  min-width: 112px;
  text-align: right;
}
.order-total strong {
  display: block;
  margin-top: 5px;
  color: #e15d3d;
  font-size: 23px;
}
.room-detail {
  border-top: 1px solid #edf1f5;
  border-bottom: 1px solid #edf1f5;
  background: #fcfdfd;
}
.room-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}
.room-heading h3 {
  margin: 6px 0 0;
  color: #20384f;
  font-size: 17px;
}
.nights {
  color: #168b8b;
  font-size: 13px;
  font-weight: 700;
}
.room-facts {
  margin-top: 12px;
  color: #637488;
  font-size: 12px;
}
.room-facts span {
  padding-right: 10px;
  border-right: 1px solid #dfe6ed;
}
.room-facts span:last-child {
  border-right: 0;
}
.room-facts svg {
  width: 14px;
  vertical-align: -3px;
  color: #168b8b;
}
.stay-timeline {
  display: grid;
  grid-template-columns: 1fr 56px 1fr;
  align-items: center;
}
.stay-point {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stay-point > div {
  display: grid;
  gap: 4px;
}
.stay-point strong {
  color: #20384f;
  font-size: 14px;
}
.stay-point small {
  color: #8390a0;
  font-size: 11px;
  font-weight: 400;
}
.point-mark {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: #fff !important;
  background: #168b8b;
  font-size: 12px !important;
}
.timeline-line {
  height: 1px;
  margin: 0 12px;
  background: #c8d6df;
}
.order-information {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  border-top: 1px solid #edf1f5;
}
.order-information > div {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 10px;
  align-items: center;
}
.order-information span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  grid-column: 1 / -1;
}
.order-information span svg {
  width: 14px;
  color: #168b8b;
}
.order-information strong {
  color: #30465a;
  font-size: 13px;
}
.order-information small {
  color: #8190a0;
  font-size: 12px;
}
.order-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px solid #edf1f5;
}
.button-quiet,
.button-primary {
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  text-decoration: none;
}
.button-quiet {
  border: 1px solid #d5dfe7;
  color: #536577;
  background: #fff;
}
.button-primary {
  border: 1px solid #168b8b;
  color: #fff;
  background: #168b8b;
}
@media (max-width: 680px) {
  .order-card-header,
  .order-card-footer {
    align-items: flex-start;
    flex-direction: column;
  }
  .hotel-summary {
    grid-template-columns: 88px minmax(0, 1fr);
    gap: 12px;
  }
  .hotel-cover {
    width: 88px;
    height: 78px;
  }
  .order-total {
    grid-column: 2;
    min-width: 0;
    text-align: left;
  }
  .order-total strong {
    display: inline;
    margin-left: 7px;
    font-size: 18px;
  }
  .stay-timeline {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .timeline-line {
    width: 1px;
    height: 12px;
    margin: 0 0 0 13px;
  }
  .order-information {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
@media (max-width: 420px) {
  .order-card-header,
  .hotel-summary,
  .room-detail,
  .stay-timeline,
  .order-information,
  .order-card-footer {
    padding-right: 14px;
    padding-left: 14px;
  }
  .hotel-name {
    font-size: 16px;
  }
  .hotel-actions {
    gap: 6px;
  }
  .hotel-actions button,
  .hotel-actions a {
    padding: 5px 6px;
  }
  .footer-actions {
    width: 100%;
  }
  .button-quiet,
  .button-primary {
    flex: 1;
    text-align: center;
  }
}
</style>
