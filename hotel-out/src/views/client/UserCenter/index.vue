<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import { useOrderPage } from '@/composables/useOrderPage'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import RateStar from '@/components/business/RateStar.vue'
import Empty from '@/components/common/Empty.vue'
import NavHeader from '@/components/common/NavHeader.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ProfileEditor from '@/components/business/ProfileEditor.vue'
import OrderDetailCard from '@/components/business/OrderDetailCard.vue'
import { useUserStore } from '@/stores'
import {
  cancelBooking,
  deleteUserComment,
  getOrderPage,
  getUserComments,
  getUserFavoriteHotels,
  getUserFavorites,
  refundBooking,
  setUserFavorite,
  updateUserComment,
} from '@/services/dataSource'

const route = useRoute()
const userStore = useUserStore()
const {
  rows: orders,
  total,
  pages,
  page,
  loading: orderLoading,
  error: orderError,
  load: loadOrders,
  changePage,
  filter: filterOrders,
} = useOrderPage(getOrderPage, false)
const orderFilter = ref('全部订单')
const orderFilters = [
  '全部订单',
  '待支付',
  '待确认',
  '待入住',
  '已入住',
  '已完成',
  '退款中',
  '已退款',
  '已取消',
]
const statusCodes = {
  待支付: 'PENDING_PAYMENT',
  待确认: 'PAID',
  待入住: 'PENDING_CHECKIN',
  已入住: 'CHECKED_IN',
  已完成: 'COMPLETED',
  退款中: 'REFUNDING',
  已退款: 'REFUNDED',
  已取消: 'CANCELLED',
}
const userReviews = ref([])
const favoriteIds = ref([])
const apiFavoriteHotels = ref([])
const profile = computed(() => ({ name: '', phone: '', verified: false, ...userStore.userInfo }))
const active = computed(() => String(route.meta.section || 'orders'))
const favoriteHotels = computed(() =>
  apiFavoriteHotels.value.filter((hotel) => favoriteIds.value.includes(hotel.id)),
)
const filteredOrders = orders
const { loading, error, load } = useLoadState(
  async (isCurrent) => {
    if (active.value === 'favorites') {
      const [ids, hotels] = await Promise.all([getUserFavorites(), getUserFavoriteHotels()])
      if (isCurrent()) {
        favoriteIds.value = ids
        apiFavoriteHotels.value = hotels
      }
    } else if (active.value === 'reviews') {
      const reviews = await getUserComments()
      if (isCurrent()) userReviews.value = reviews
    }
  },
  () => {
    userReviews.value = []
    favoriteIds.value = []
    apiFavoriteHotels.value = []
  },
)
function loadSection() {
  return active.value === 'orders' ? loadOrders() : load()
}
function selectFilter(value) {
  orderFilter.value = value
  filterOrders(statusCodes[value] || '')
}
watch(active, loadSection, { immediate: true })

async function cancelApi(id) {
  await cancelBooking(id, '用户取消')
  await loadOrders()
  ElMessage.success('订单已取消')
}

async function refundApi(order) {
  Object.assign(order, await refundBooking(order.id))
  ElMessage.success('退款申请已提交')
}

function contactMerchant() {
  ElMessage.info('商家联系方式将在订单确认后提供，您也可以通过房源详情发送私信')
}

async function removeFavoriteApi(id) {
  favoriteIds.value = await setUserFavorite(id, false)
}

async function editReviewApi(review) {
  const { value } = await ElMessageBox.prompt('修改评价内容', '编辑评价', {
    inputValue: review.content,
    inputPattern: /\S+/,
    inputErrorMessage: '评价内容不能为空',
  })
  Object.assign(review, await updateUserComment(review.id, { ...review, content: value }))
  ElMessage.success('评价已更新')
}

async function deleteReviewApi(review) {
  await ElMessageBox.confirm('确认删除这条评价？', '删除评价', { type: 'warning' })
  await deleteUserComment(review.id)
  userReviews.value = userReviews.value.filter((item) => item.id !== review.id)
  ElMessage.success('评价已删除')
}

function updateSecurity(type) {
  ElMessage.info(type === 'password' ? '密码修改尚未开放' : '请在个人信息页修改手机号')
}
</script>

<template>
  <div class="center-page">
    <div class="page-header"><NavHeader /></div>
    <main class="center-main">
      <aside class="center-nav">
        <div class="profile-mini">
          <UserAvatar :src="profile.avatar || ''" :name="profile.name || '住客'" />
          <strong>{{ profile.name || '住客' }}</strong
          ><small>{{ profile.verified ? '已实名认证' : '未认证' }}</small>
        </div>
        <nav>
          <RouterLink to="/user/recharge">账户充值</RouterLink>
          <RouterLink to="/messages">我的私信</RouterLink>
          <RouterLink to="/user/orders">我的订单</RouterLink
          ><RouterLink to="/user/favorites">我的收藏</RouterLink
          ><RouterLink to="/user/reviews">我的评价</RouterLink
          ><RouterLink to="/user/profile">个人信息</RouterLink
          ><RouterLink to="/user/security">账户安全</RouterLink>
        </nav>
      </aside>
      <section class="center-content">
        <LoadState
          :loading="active === 'orders' ? orderLoading : loading"
          :error="active === 'orders' ? orderError : error"
          @retry="loadSection"
        />
        <template v-if="active === 'orders'"
          ><div class="section-title">
            <div>
              <p>MY TRIPS</p>
              <h1>我的订单</h1>
            </div>
            <span>共 {{ total }} 笔</span>
          </div>
          <div class="order-tabs">
            <button
              v-for="filter in orderFilters"
              :key="filter"
              :class="{ active: orderFilter === filter }"
              type="button"
              @click="selectFilter(filter)"
            >
              {{ filter }}
            </button>
          </div>
          <div class="order-list">
            <OrderDetailCard
              v-for="order in filteredOrders"
              :key="order.id"
              :order="order"
              @cancel="cancelApi(order.id)"
              @refund="refundApi(order)"
              @contact="contactMerchant"
            />
          </div>
          <Empty
            v-if="!orderLoading && !orderError && !orders.length"
            title="暂无订单"
            description="选择合适的住处后，你的预订会显示在这里。"
          />
          <div v-if="pages > 1" class="order-pagination">
            <button :disabled="orderLoading || page <= 1" @click="changePage(page - 1)">
              上一页
            </button>
            <span>{{ page }} / {{ pages }}</span>
            <button :disabled="orderLoading || page >= pages" @click="changePage(page + 1)">
              下一页
            </button>
          </div></template
        >
        <template v-else-if="active === 'recharge'"
          ><div class="section-title">
            <div>
              <p>ACCOUNT WALLET</p>
              <h1>账户充值</h1>
            </div>
          </div>
          <section class="recharge-panel">
            <div class="recharge-balance">
              <span>当前账户余额</span>
              <strong>¥{{ Number(userStore.userInfo?.balance || 0).toFixed(2) }}</strong>
            </div>
            <div class="recharge-options">
              <button type="button" disabled>¥100</button>
              <button type="button" disabled>¥300</button>
              <button type="button" disabled>¥500</button>
            </div>
            <p>充值支付通道正在接入，账户余额功能开放后即可使用。</p>
          </section></template
        >
        <template v-else-if="active === 'favorites'"
          ><div class="section-title">
            <div>
              <p>SAVED STAYS</p>
              <h1>我的收藏</h1>
            </div>
          </div>
          <div v-if="favoriteHotels.length" class="favorite-grid">
            <article v-for="hotel in favoriteHotels" :key="hotel.id">
              <img :src="hotel.image" :alt="hotel.name" />
              <div>
                <h2>{{ hotel.name }}</h2>
                <p>{{ hotel.city }} · ¥{{ hotel.price }}/晚起</p>
                <div>
                  <RouterLink :to="{ name: 'hotel-detail', params: { id: hotel.id } }"
                    >查看详情</RouterLink
                  ><button type="button" @click="removeFavoriteApi(hotel.id)">取消收藏</button>
                </div>
              </div>
            </article>
          </div>
          <Empty
            v-else-if="!loading && !error"
            title="还没有收藏"
            description="遇到喜欢的房源时，点击收藏即可保存在这里。"
        /></template>
        <template v-else-if="active === 'reviews'"
          ><div class="section-title">
            <div>
              <p>YOUR REVIEWS</p>
              <h1>我的评价</h1>
            </div>
          </div>
          <article v-for="review in userReviews" :key="review.id" class="review-item">
            <div>
              <h2>{{ review.hotel }}</h2>
              <RateStar :model-value="review.score" readonly />
            </div>
            <p>{{ review.content }}</p>
            <div class="review-actions">
              <button type="button" @click="editReviewApi(review)">修改评价</button
              ><button type="button" @click="deleteReviewApi(review)">删除评价</button>
            </div>
          </article></template
        >
        <template v-else-if="active === 'profile'"
          ><div class="section-title">
            <div>
              <p>PERSONAL INFO</p>
              <h1>个人信息</h1>
            </div>
          </div>
          <ProfileEditor allow-phone
        /></template>
        <template v-else
          ><div class="section-title">
            <div>
              <p>ACCOUNT SECURITY</p>
              <h1>账户安全</h1>
            </div>
          </div>
          <div class="security-list">
            <article>
              <div>
                <h2>登录密码</h2>
                <p>建议定期更新密码，保护账号安全。</p>
              </div>
              <button type="button" @click="updateSecurity('password')">修改密码</button>
            </article>
            <article>
              <div>
                <h2>绑定手机</h2>
                <p>{{ profile.phone || '尚未绑定' }}</p>
              </div>
              <button type="button" @click="updateSecurity('phone')">更换手机</button>
            </article>
          </div></template
        >
      </section>
    </main>
  </div>
</template>

<style scoped>
.order-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}
.order-pagination button {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  background: white;
  color: var(--brand);
  cursor: pointer;
}
.center-page {
  min-height: 100vh;
  color: var(--ink);
  background: #f5f7fa;
}
.page-header {
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.center-main {
  display: grid;
  grid-template-columns: 230px 1fr;
  gap: 28px;
  width: min(100% - 48px, 1180px);
  margin: 0 auto;
  padding: 42px 0 100px;
}
.center-nav {
  min-width: 0;
  align-self: start;
  padding: 22px;
  border: 1px solid var(--line);
  background: #fff;
}
.profile-mini {
  display: grid;
  justify-items: center;
  padding-bottom: 22px;
  border-bottom: 1px solid #e4e7eb;
}
.profile-mini > span {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--brand);
  font-size: 22px;
}
.profile-mini strong {
  margin-top: 10px;
  max-width: 100%;
  overflow-wrap: anywhere;
  text-align: center;
}
.profile-mini small {
  margin-top: 5px;
  color: #2f9e83;
}
.center-nav nav {
  display: grid;
  margin-top: 16px;
}
.center-nav a {
  padding: 12px;
  color: #52606d;
  text-decoration: none;
}
.center-nav a.router-link-active {
  color: var(--brand);
  background: #e9f5f3;
  font-weight: 700;
}
.center-content {
  min-width: 0;
  padding: 30px;
  border: 1px solid var(--line);
  background: #fff;
}
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: end;
  padding-bottom: 24px;
  border-bottom: 1px solid #e4e7eb;
}
.section-title p {
  margin: 0 0 8px;
  color: var(--brand);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
}
.section-title h1 {
  margin: 0;
  font-family: var(--font-ui);
  font-size: 36px;
  font-weight: 400;
}
.section-title > span {
  color: var(--muted);
  font-size: 12px;
}
.recharge-panel {
  max-width: 680px;
  margin-top: 28px;
  padding: 26px;
  border: 1px solid var(--line);
  background: #f7fbff;
}
.recharge-balance {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
  color: var(--muted);
  font-size: 13px;
}
.recharge-balance strong {
  color: var(--brand);
  font-size: 34px;
}
.recharge-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 22px;
}
.recharge-options button {
  min-height: 48px;
  border: 1px solid #cbd5e1;
  color: var(--muted);
  background: #fff;
  cursor: not-allowed;
}
.recharge-panel > p {
  margin: 18px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.order-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
  padding: 18px 0;
}
.order-tabs button {
  padding: 6px 0;
  border: 0;
  border-bottom: 2px solid transparent;
  background: none;
  cursor: pointer;
}
.order-tabs button.active {
  border-color: var(--brand);
  color: var(--brand);
}
.order-list {
  display: grid;
  gap: 12px;
}
.review-item,
.security-list article {
  border: 1px solid var(--line);
}
.favorite-grid button,
.favorite-grid a,
.review-item button,
.security-list button {
  padding: 7px 11px;
  border: 1px solid #cbd5e1;
  color: #52606d;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  text-decoration: none;
}
.review-actions {
  display: flex;
  gap: 8px;
}
.favorite-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-top: 24px;
}
.favorite-grid article {
  overflow: hidden;
  border: 1px solid var(--line);
}
.favorite-grid img {
  width: 100%;
  aspect-ratio: 1.7;
  object-fit: cover;
}
.favorite-grid article > div {
  padding: 16px;
}
.favorite-grid h2 {
  margin: 0;
}
.favorite-grid p {
  color: var(--muted);
}
.favorite-grid article div div {
  display: flex;
  gap: 8px;
}
.review-item {
  margin-top: 16px;
  padding: 18px;
}
.review-item > div {
  display: flex;
  justify-content: space-between;
}
.review-item h2 {
  margin: 0;
}
.review-item p {
  color: #52606d;
  line-height: 1.7;
}
.profile-form {
  display: grid;
  gap: 16px;
  max-width: 560px;
  margin-top: 28px;
}
.profile-form label {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}
.profile-form input {
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
}
.profile-form button {
  width: 140px;
  min-height: 44px;
  border: 0;
  color: #fff;
  background: var(--brand);
  cursor: pointer;
}
.security-list {
  display: grid;
  gap: 12px;
  margin-top: 24px;
}
.security-list article {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px;
}
.security-list h2 {
  margin: 0 0 6px;
  font-size: 17px;
}
.security-list p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}
@media (max-width: 800px) {
  .center-main {
    grid-template-columns: minmax(0, 1fr);
  }
  .center-nav nav {
    grid-template-columns: repeat(6, 1fr);
    overflow: auto;
  }
  .profile-mini {
    display: none;
  }
  .center-nav a {
    white-space: nowrap;
    text-align: center;
  }
}
@media (max-width: 600px) {
  .center-main {
    width: min(100% - 24px, 1180px);
    padding-top: 20px;
  }
  .center-nav {
    padding: 8px;
  }
  .center-content {
    padding: 20px;
  }
  .favorite-grid {
    grid-template-columns: 1fr;
  }
  .center-nav nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
