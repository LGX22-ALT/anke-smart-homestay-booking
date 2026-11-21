<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import DateRangePicker from '@/components/business/DateRangePicker.vue'
import RateStar from '@/components/business/RateStar.vue'
import Footer from '@/components/common/Footer.vue'
import NavHeader from '@/components/common/NavHeader.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import {
  getAvailability,
  getHotel,
  getUserFavorites,
  setUserFavorite,
  useMock,
} from '@/services/dataSource'
import { useBookingStore, useUserStore } from '@/stores'
import { getStayNights } from '@/utils/date'

import { recordHotel } from '@/utils/travelStorage'
import { useSectionNavigation } from '@/composables/useSectionNavigation'
const route = useRoute()
const router = useRouter()
const bookingStore = useBookingStore()
const userStore = useUserStore()

const hotel = ref({
  name: '',
  city: '',
  district: '',
  address: '',
  score: 0,
  gallery: [],
  facilities: [],
  rooms: [],
  reviews: [],
  nearby: [],
  merchant: null,
})
const selectedRoomId = ref(null)
const roomFilter = ref('all')
const checkIn = ref(route.query.checkIn || dayjs().add(1, 'day').format('YYYY-MM-DD'))
const checkOut = ref(route.query.checkOut || dayjs().add(2, 'day').format('YYYY-MM-DD'))
const guests = ref(Number(route.query.guests || 2))
const favorite = ref(false)
const previewImage = ref('')
const availabilityRows = ref([])
const availabilityLoading = ref(false)
const availabilityChecked = ref(false)
const availabilityError = ref('')
let availabilityRequestId = 0

const nights = computed(() => getStayNights(checkIn.value, checkOut.value))
const reviewFilter = ref('all')
const reviewPage = ref(1)
const reviewsPerPage = 5
const reviews = computed(() =>
  (hotel.value.reviews || []).map((review) => {
    let images = review.images || []
    if (typeof images === 'string') {
      try {
        images = JSON.parse(images)
      } catch {
        images = []
      }
    }
    return {
      ...review,
      user: review.user || review.userName || '住客',
      images: Array.isArray(images)
        ? images.filter((image) => typeof image === 'string' && image)
        : [],
      reply: review.reply || review.merchantReply || '',
      date: review.date || review.createTime || '',
      score: Math.max(0, Math.min(5, Number(review.score) || 0)),
    }
  }),
)
const reviewAverage = computed(() =>
  reviews.value.length
    ? (
        reviews.value.reduce((total, review) => total + review.score, 0) / reviews.value.length
      ).toFixed(1)
    : '—',
)
const reviewScoreLabel = computed(() => {
  if (!reviews.value.length) return '暂无评分'
  if (Number(reviewAverage.value) >= 4.5) return '超棒'
  if (Number(reviewAverage.value) >= 4) return '很好'
  if (Number(reviewAverage.value) >= 3) return '不错'
  return '有待改善'
})
const scoreDistribution = computed(() =>
  [5, 4, 3, 2, 1].map((score) => {
    const count = reviews.value.filter((review) => Math.round(review.score) === score).length
    return {
      score,
      count,
      percent: reviews.value.length ? (count / reviews.value.length) * 100 : 0,
    }
  }),
)
const reviewFilters = computed(() => [
  { key: 'all', label: '全部', count: reviews.value.length },
  { key: 'latest', label: '最新', count: reviews.value.length },
  { key: 'good', label: '好评', count: reviews.value.filter((review) => review.score >= 4).length },
  {
    key: 'photos',
    label: '有图',
    count: reviews.value.filter((review) => review.images.length).length,
  },
  {
    key: 'critical',
    label: '低分评价',
    count: reviews.value.filter((review) => review.score < 3).length,
  },
])
const filteredReviews = computed(() => {
  const result = reviews.value.filter((review) => {
    if (reviewFilter.value === 'good') return review.score >= 4
    if (reviewFilter.value === 'photos') return review.images.length > 0
    if (reviewFilter.value === 'critical') return review.score < 3
    return true
  })
  return reviewFilter.value === 'latest'
    ? [...result].sort((a, b) => String(b.date).localeCompare(String(a.date)))
    : result
})
const reviewPages = computed(() => Math.ceil(filteredReviews.value.length / reviewsPerPage))
const visibleReviews = computed(() =>
  filteredReviews.value.slice(
    (reviewPage.value - 1) * reviewsPerPage,
    reviewPage.value * reviewsPerPage,
  ),
)
watch(reviewFilter, () => {
  reviewPage.value = 1
})
const galleryImages = computed(() => {
  const images = (hotel.value.gallery || []).filter(Boolean)
  if (!images.length && hotel.value.image) images.push(hotel.value.image)
  return [...new Set(images)].slice(0, 3)
})
const galleryClass = computed(() => ({
  single: galleryImages.value.length <= 1,
  double: galleryImages.value.length === 2,
}))

const roomOptions = computed(() =>
  (hotel.value.rooms || []).map((room, index) => {
    const calendar = availabilityRows.value.filter(
      (item) => Number(item.roomTypeId) === Number(room.id),
    )
    const calendarComplete = nights.value > 0 && calendar.length === nights.value
    const fallbackStock = Number(room.stock || 0)
    const remaining = useMock
      ? fallbackStock
      : calendarComplete
        ? Math.min(...calendar.map((item) => Number(item.availableStock || 0)))
        : 0
    const calendarAvailable = calendarComplete
      ? calendar.every((item) => Number(item.isAvailable) === 1 && Number(item.availableStock) > 0)
      : false
    const priceTotal = calendarComplete
      ? calendar.reduce((sum, item) => sum + Number(item.price || 0), 0)
      : Number(room.price || 0) * nights.value
    const nightlyPrice = calendarComplete
      ? Math.round(priceTotal / Math.max(nights.value, 1))
      : Number(room.price || 0)
    const inventoryReady = useMock || availabilityChecked.value
    const available =
      inventoryReady &&
      !availabilityError.value &&
      (useMock ? fallbackStock > 0 : calendarAvailable) &&
      remaining > 0

    return {
      ...room,
      image:
        room.image ||
        galleryImages.value[index % Math.max(galleryImages.value.length, 1)] ||
        hotel.value.image,
      remaining,
      nightlyPrice,
      stayTotal: priceTotal,
      available,
      guestCompatible: guests.value <= Number(room.guests || 1),
    }
  }),
)

const roomFilters = computed(() => {
  const filters = [{ key: 'all', label: '全部房型' }]
  const beds = [...new Set(roomOptions.value.map((room) => room.bed).filter(Boolean))]
  beds.forEach((bed) => filters.push({ key: bed, label: bed }))
  return filters
})
const visibleRooms = computed(() =>
  roomFilter.value === 'all'
    ? roomOptions.value
    : roomOptions.value.filter((room) => room.bed === roomFilter.value),
)
const { loading, error, load } = useLoadState(
  async (isCurrent) => {
    const result = await getHotel(route.params.id)
    if (!isCurrent()) return
    if (!result) throw new Error('房源不存在或已下架')
    hotel.value = result
    recordHotel(hotel.value)
    selectedRoomId.value = hotel.value.rooms[0]?.id || null
    await refreshAvailability()
    if (userStore.isAuthenticated && userStore.role === 'user') {
      favorite.value = (await getUserFavorites()).includes(hotel.value.id)
    }
  },
  () => {
    hotel.value = { rooms: [], reviews: [], gallery: [], facilities: [], nearby: [] }
  },
)
onMounted(load)
watch(() => route.params.id, load)

watch([checkIn, checkOut], () => {
  if (hotel.value.id) refreshAvailability()
})

async function refreshAvailability() {
  const requestId = ++availabilityRequestId
  availabilityRows.value = []
  availabilityChecked.value = false
  availabilityError.value = ''
  if (!nights.value) {
    availabilityError.value = '离店日期必须晚于入住日期'
    return
  }
  availabilityLoading.value = true
  try {
    const rows = await getAvailability(hotel.value.id, {
      checkIn: checkIn.value,
      checkOut: checkOut.value,
    })
    if (requestId !== availabilityRequestId) return
    availabilityRows.value = Array.isArray(rows) ? rows : []
    availabilityChecked.value = true
  } catch {
    if (requestId !== availabilityRequestId) return
    availabilityError.value = '房态暂时无法查询，请稍后重试'
  } finally {
    if (requestId === availabilityRequestId) availabilityLoading.value = false
  }
}

function book(room) {
  selectedRoomId.value = room.id
  if (!nights.value) return ElMessage.warning('请选择正确的入住和离店日期')
  if (!room.guestCompatible) return ElMessage.warning(`该房型最多入住 ${room.guests} 人`)
  if (!room.available) return ElMessage.warning('该日期房型已满，请选择其他房型或日期')
  bookingStore.setBooking({
    hotel: hotel.value,
    room: { ...room, price: room.nightlyPrice },
    checkInDate: checkIn.value,
    checkOutDate: checkOut.value,
    guestCount: guests.value,
  })
  router.push({
    name: userStore.isAuthenticated ? 'order-confirm' : 'client-login',
    query: userStore.isAuthenticated ? {} : { redirect: '/order/confirm' },
  })
}

function stockText(room) {
  if (availabilityLoading.value) return '正在查询房态'
  if (availabilityError.value) return '房态查询失败'
  if (!room.guestCompatible) return `最多入住 ${room.guests} 人`
  if (!room.available) return '所选日期已满房'
  return room.remaining <= 3 ? `仅剩 ${room.remaining} 间` : `${room.remaining} 间可订`
}

const detailSections = [
  { id: 'rooms', label: '房型' },
  { id: 'reviews', label: '评价' },
  { id: 'overview', label: '介绍' },
  { id: 'merchant', label: '商家' },
  { id: 'facilities', label: '设施' },
  { id: 'surroundings', label: '周边' },
]
const { sectionNav, activeSection, indicatorStyle, scrollToSection } = useSectionNavigation(
  detailSections,
  () => hotel.value.id,
)

async function changeFavorite() {
  if (!userStore.isAuthenticated)
    return router.push({ name: 'client-login', query: { redirect: route.fullPath } })
  if (userStore.role !== 'user') return ElMessage.warning('请使用住客账号收藏房源')
  favorite.value = (await setUserFavorite(hotel.value.id, !favorite.value)).includes(hotel.value.id)
}

function openMessages() {
  if (userStore.isAuthenticated && userStore.role !== 'user')
    return ElMessage.warning('请使用住客账号私信商家')
  router.push({ name: 'client-messages', params: { hotelId: hotel.value.id } })
}
</script>

<template>
  <div class="detail-page">
    <div class="detail-header"><NavHeader /></div>
    <main class="detail-main">
      <LoadState :loading="loading" :error="error" @retry="load" />
      <template v-if="!loading && !error">
        <div class="breadcrumb">
          <RouterLink to="/">首页</RouterLink><span>/</span
          ><RouterLink to="/search">房源</RouterLink><span>/</span><span>{{ hotel.name }}</span>
        </div>

        <section class="title-row">
          <div class="title-copy">
            <p class="location-line">
              <span>{{ hotel.type || '精品民宿' }}</span
              >{{ hotel.city }} · {{ hotel.district }}
            </p>
            <h1>{{ hotel.name }}</h1>
            <div class="title-meta">
              <span class="rating"
                ><RateStar :model-value="Math.round(hotel.score)" readonly /><b>{{
                  hotel.score
                }}</b></span
              >
              <span class="meta-divider"></span>
              <span>{{ hotel.address }}</span>
              <span v-if="hotel.merchant" class="verified-inline">商家已认证</span>
            </div>
          </div>
          <button
            type="button"
            :class="['favorite-button', { active: favorite }]"
            :title="userStore.isAuthenticated ? '收藏房源' : '登录后收藏房源'"
            @click="changeFavorite"
          >
            <span aria-hidden="true">{{ favorite ? '♥' : '♡' }}</span
            ><span class="favorite-text">{{ favorite ? '已收藏' : '收藏' }}</span>
          </button>
        </section>

        <section :class="['gallery', galleryClass]">
          <button
            v-for="(image, index) in galleryImages"
            :key="image"
            type="button"
            @click="previewImage = image"
          >
            <img :src="image" :alt="`${hotel.name} 图片 ${index + 1}`" />
            <span v-if="index === galleryImages.length - 1">查看全部图片</span>
          </button>
          <div v-if="!galleryImages.length" class="gallery-empty">商家暂未上传房源图片</div>
        </section>

        <div
          v-if="previewImage"
          class="image-preview"
          role="dialog"
          aria-label="房源图片预览"
          @click.self="previewImage = ''"
        >
          <button type="button" aria-label="关闭预览" @click="previewImage = ''">×</button>
          <img :src="previewImage" :alt="hotel.name" />
        </div>

        <nav ref="sectionNav" class="section-nav" aria-label="房源详情导航">
          <button
            v-for="section in detailSections"
            :key="section.id"
            type="button"
            :data-section="section.id"
            :class="{ active: activeSection === section.id }"
            :aria-current="activeSection === section.id ? 'location' : undefined"
            :aria-controls="section.id"
            @click="scrollToSection(section.id)"
          >
            {{ section.label }}
          </button>
          <span class="section-indicator" :style="indicatorStyle" aria-hidden="true"></span>
        </nav>

        <section id="rooms" class="room-inventory anchor-section">
          <div class="section-heading">
            <div>
              <p class="eyebrow">ROOM AVAILABILITY</p>
              <h2>选择房型</h2>
              <p>库存会根据入住日期实时计算，预订后系统将自动锁定对应房间。</p>
            </div>
            <span class="inventory-count">{{ hotel.rooms.length }} 种房型</span>
          </div>

          <div class="availability-search">
            <DateRangePicker
              v-model:check-in="checkIn"
              v-model:check-out="checkOut"
              :min-date="dayjs().format('YYYY-MM-DD')"
            />
            <label class="guest-field">
              <span>入住人数</span>
              <select v-model.number="guests">
                <option :value="1">1 位住客</option>
                <option :value="2">2 位住客</option>
                <option :value="3">3 位住客</option>
                <option :value="4">4 位住客</option>
                <option :value="5">5 位住客</option>
                <option :value="6">6 位住客</option>
              </select>
            </label>
            <div class="stay-summary">
              <span>本次入住</span><strong>{{ nights || 0 }} 晚</strong>
              <small>{{ checkIn }} 至 {{ checkOut }}</small>
            </div>
          </div>

          <p v-if="availabilityError" class="availability-error">{{ availabilityError }}</p>

          <div class="room-filters" aria-label="房型筛选">
            <button
              v-for="filter in roomFilters"
              :key="filter.key"
              type="button"
              :class="{ active: roomFilter === filter.key }"
              @click="roomFilter = filter.key"
            >
              {{ filter.label }}
            </button>
          </div>

          <div class="room-card-list">
            <article
              v-for="room in visibleRooms"
              :key="room.id"
              :class="['room-card', { selected: selectedRoomId === room.id }]"
              @click="selectedRoomId = room.id"
            >
              <button class="room-image" type="button" @click.stop="previewImage = room.image">
                <img v-if="room.image" :src="room.image" :alt="room.name" />
                <span v-else>暂无房型图片</span>
              </button>
              <div class="room-copy">
                <div class="room-title-line">
                  <div>
                    <span v-if="selectedRoomId === room.id" class="recommend-badge">当前选择</span>
                    <h3>{{ room.name }}</h3>
                  </div>
                  <span
                    :class="[
                      'stock-badge',
                      { urgent: room.remaining <= 3 && room.available, sold: !room.available },
                    ]"
                  >
                    {{ stockText(room) }}
                  </span>
                </div>
                <p class="room-specs">
                  <span>{{ room.area || 32 }} m²</span><span>{{ room.bed || '舒适床型' }}</span
                  ><span>可住 {{ room.guests }} 人</span>
                </p>
                <div class="room-benefits">
                  <span>入住前 3 天可取消</span><span>在线预订</span><span>到店无需押金</span>
                </div>
                <div v-if="room.facilities?.length" class="room-facilities">
                  <span v-for="facility in room.facilities.slice(0, 4)" :key="facility">{{
                    facility
                  }}</span>
                </div>
                <p class="room-note">
                  实际房间由商家根据当日房态安排，图片与设施以该房型说明为准。
                </p>
              </div>
              <div class="room-commerce">
                <span>每晚均价</span>
                <p>
                  <small>¥</small><strong>{{ room.nightlyPrice }}</strong
                  ><em>/晚</em>
                </p>
                <small>{{ nights }} 晚合计 ¥{{ room.stayTotal }}</small>
                <button
                  type="button"
                  :disabled="availabilityLoading || !room.available || !room.guestCompatible"
                  @click.stop="book(room)"
                >
                  {{ room.available && room.guestCompatible ? '预订此房型' : '暂不可订' }}
                </button>
              </div>
            </article>
            <div v-if="!visibleRooms.length" class="room-empty">没有符合筛选条件的房型。</div>
          </div>
          <section id="reviews" class="anchor-section guest-reviews">
            <header class="reviews-heading">
              <h2>
                用户点评 <span>({{ reviews.length }})</span>
              </h2>
              <span>来自住客的真实入住感受</span>
            </header>
            <div class="review-overview">
              <div class="review-overall">
                <div>
                  <strong>{{ reviewAverage }}</strong
                  ><span>{{ reviewScoreLabel }}</span>
                </div>
                <p>
                  {{
                    reviews.length
                      ? `${reviews.length} 条住客点评 · 满分 5 分`
                      : '期待第一位住客分享入住体验'
                  }}
                </p>
              </div>
              <div class="score-distribution" aria-label="住客评分分布">
                <div
                  v-for="item in scoreDistribution"
                  :key="item.score"
                  class="score-distribution-row"
                >
                  <span>{{ item.score }} 星</span>
                  <div class="score-track" :aria-label="`${item.score}星评价 ${item.count} 条`">
                    <span :style="{ width: `${item.percent}%` }"></span>
                  </div>
                  <b>{{ item.count }}</b>
                </div>
              </div>
            </div>
            <div class="review-filters" aria-label="评价筛选">
              <button
                v-for="filter in reviewFilters"
                :key="filter.key"
                type="button"
                :class="{ active: reviewFilter === filter.key }"
                :aria-pressed="reviewFilter === filter.key"
                @click="reviewFilter = filter.key"
              >
                {{ filter.label }} <span>({{ filter.count }})</span>
              </button>
            </div>
            <div class="review-results" aria-live="polite">
              <article v-for="review in visibleReviews" :key="review.id" class="guest-review">
                <header>
                  <div class="review-author">
                    <span class="review-avatar" aria-hidden="true">{{
                      review.user.slice(0, 1)
                    }}</span>
                    <div>
                      <strong>{{ review.user }}</strong
                      ><span>{{ review.roomName || '住客点评' }}</span>
                    </div>
                  </div>
                  <div class="review-score">
                    <strong>{{ review.score.toFixed(1) }}</strong
                    ><RateStar :model-value="review.score" readonly />
                  </div>
                </header>
                <p class="review-content">{{ review.content }}</p>
                <div v-if="review.images.length" class="review-photos">
                  <button
                    v-for="(image, index) in review.images"
                    :key="`${image}-${index}`"
                    type="button"
                    :aria-label="`查看点评图片 ${index + 1}`"
                    @click="previewImage = image"
                  >
                    <img :src="image" :alt="`住客点评图片 ${index + 1}`" loading="lazy" />
                  </button>
                </div>
                <time v-if="review.date" class="review-date"
                  >{{ dayjs(review.date).format('YYYY年MM月DD日') }}发布</time
                >
                <blockquote v-if="review.reply" class="merchant-review-reply">
                  <strong>商家回复</strong>
                  <p>{{ review.reply }}</p>
                </blockquote>
              </article>
              <div v-if="!visibleReviews.length" class="review-empty">
                <strong>{{ reviews.length ? '暂无符合条件的点评' : '还没有住客点评' }}</strong>
                <p>
                  {{
                    reviews.length
                      ? '试试其他筛选条件，查看住客的入住体验。'
                      : '完成入住后，欢迎分享你的真实体验。'
                  }}
                </p>
                <button v-if="reviewFilter !== 'all'" type="button" @click="reviewFilter = 'all'">
                  查看全部点评
                </button>
              </div>
            </div>
            <div v-if="reviewPages > 1" class="review-pagination">
              <button type="button" :disabled="reviewPage === 1" @click="reviewPage--">
                上一页
              </button>
              <span>{{ reviewPage }} / {{ reviewPages }}</span>
              <button type="button" :disabled="reviewPage === reviewPages" @click="reviewPage++">
                下一页
              </button>
            </div>
          </section>
        </section>

        <div class="detail-layout">
          <div class="detail-content">
            <section id="overview" class="anchor-section content-section">
              <p class="eyebrow">ABOUT THE STAY</p>
              <h2>关于这间住处</h2>
              <p class="description">{{ hotel.description }}</p>
              <div class="feature-strip">
                <span
                  ><strong>{{ hotel.rooms.length }}</strong> 种房型</span
                >
                <span
                  ><strong>{{ hotel.score }}</strong> 住客评分</span
                >
                <span><strong>14:00</strong> 入住时间</span>
              </div>
            </section>

            <section id="merchant" class="anchor-section content-section merchant-section">
              <p class="eyebrow">YOUR HOST</p>
              <h2>认识这家民宿的商家</h2>
              <article v-if="hotel.merchant" class="merchant-card">
                <UserAvatar
                  class="merchant-mark"
                  :src="hotel.merchant.avatar || ''"
                  :name="hotel.merchant.name || hotel.merchant.companyName || '民宿商家'"
                />
                <div class="merchant-copy">
                  <div class="merchant-title">
                    <div>
                      <strong>{{ hotel.merchant.companyName }}</strong>
                      <span
                        >联系人：{{
                          hotel.merchant.name || hotel.merchant.contactName || '商家客服'
                        }}</span
                      >
                    </div>
                    <span class="verified-badge">已认证</span>
                  </div>
                  <p>
                    该商家目前经营 {{ hotel.merchant.listingCount || 1 }} 套在营房源，联系电话
                    {{ hotel.merchant.contactPhone || '登录后可私信咨询' }}。
                  </p>
                  <button class="message-merchant-button" type="button" @click="openMessages">
                    {{ userStore.isAuthenticated ? '私信商家' : '登录后私信' }}
                  </button>
                </div>
              </article>
              <p v-else class="empty-copy">暂未找到商家公开资料。</p>
            </section>

            <section id="facilities" class="anchor-section content-section">
              <p class="eyebrow">FACILITIES</p>
              <h2>设施与服务</h2>
              <div class="facility-grid">
                <span v-for="facility in hotel.facilities" :key="facility">✓ {{ facility }}</span>
              </div>
            </section>

            <section id="surroundings" class="anchor-section content-section">
              <p class="eyebrow">AROUND THE NEIGHBORHOOD</p>
              <h2>周边与出行</h2>
              <div v-if="hotel.nearby?.length" class="nearby-list">
                <article v-for="item in hotel.nearby" :key="`${item.type}-${item.name}`">
                  <span>{{ item.type || '周边' }}</span>
                  <div>
                    <strong>{{ item.name }}</strong>
                    <p>{{ item.description || '民宿周边便捷地点' }}</p>
                  </div>
                  <b>{{ item.distance || '距离待确认' }}</b>
                </article>
              </div>
              <p v-else class="empty-copy">商家正在补充周边景点、交通和餐饮信息。</p>
            </section>

            <section id="policies" class="anchor-section content-section">
              <p class="eyebrow">HOUSE RULES</p>
              <h2>入住须知</h2>
              <div class="policy-grid">
                <p><strong>入住 / 退房</strong><span>14:00 后入住，12:00 前退房</span></p>
                <p><strong>取消政策</strong><span>入住前 3 天可免费取消</span></p>
                <p><strong>其他说明</strong><span>室内禁烟，请保持安静</span></p>
              </div>
            </section>
          </div>
        </div>
      </template>
    </main>

    <Footer />
  </div>
</template>

<style scoped>
.detail-page {
  --ink: var(--ink);
  --muted: var(--muted);
  --line: var(--line);
  --teal: var(--brand);
  --teal-soft: #e8f4f3;
  --warm: #f3aa45;
  min-height: 100vh;
  color: var(--ink);
  background: #f4f7f8;
}
.detail-header {
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.detail-main {
  width: min(100% - 40px, 1180px);
  margin: 0 auto;
  padding: 22px 0 110px;
}
.breadcrumb {
  display: flex;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
}
.breadcrumb a {
  color: #52606d;
  text-decoration: none;
}
.title-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  padding: 34px 0 26px;
}
.title-copy {
  min-width: 0;
}
.location-line {
  display: flex;
  gap: 9px;
  align-items: center;
  margin: 0 0 9px;
  color: var(--teal);
  font-size: 12px;
  font-weight: 600;
}
.location-line span {
  padding: 4px 8px;
  border-radius: 999px;
  color: #fff;
  background: var(--teal);
  font-size: 10px;
}
.title-row h1 {
  margin: 0;
  font-family: var(--font-ui);
  font-size: clamp(36px, 5vw, 56px);
  font-weight: 400;
  line-height: 1.1;
}
.title-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
  color: var(--muted);
  font-size: 12px;
}
.rating {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
.rating b {
  color: var(--ink);
}
.meta-divider {
  width: 1px;
  height: 14px;
  background: var(--line);
}
.verified-inline {
  padding: 4px 8px;
  border-radius: 999px;
  color: var(--teal);
  background: var(--teal-soft);
}
.favorite-button {
  display: inline-flex;
  min-width: 104px;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 18px;
  border: 1px solid #9fb3c8;
  border-radius: 999px;
  color: var(--ink);
  background: #fff;
  cursor: pointer;
  transition: 160ms ease;
}
.favorite-button span {
  color: #d94c4c;
  font-size: 19px;
}
.favorite-button:hover,
.favorite-button.active {
  border-color: var(--ink);
  box-shadow: 0 8px 22px rgb(16 42 67 / 10%);
}
.gallery {
  display: grid;
  height: 500px;
  grid-template-columns: 2fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 8px;
  overflow: hidden;
  border-radius: 14px;
  background: var(--line);
  box-shadow: 0 18px 50px rgb(16 42 67 / 12%);
}
.gallery.single {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}
.gallery.double {
  grid-template-columns: 3fr 2fr;
  grid-template-rows: 1fr;
}
.gallery > button {
  position: relative;
  min-width: 0;
  min-height: 0;
  padding: 0;
  border: 0;
  overflow: hidden;
  background: var(--line);
  cursor: zoom-in;
}
.gallery > button:first-child {
  grid-row: 1 / -1;
}
.gallery.single > button:first-child,
.gallery.double > button:first-child {
  grid-row: auto;
}
.gallery img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 240ms ease;
}
.gallery button:hover img {
  transform: scale(1.025);
}
.gallery button span {
  position: absolute;
  right: 16px;
  bottom: 16px;
  padding: 9px 13px;
  border-radius: 6px;
  color: var(--ink);
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 4px 12px rgb(16 42 67 / 14%);
  font-size: 11px;
}
.gallery-empty {
  display: grid;
  grid-column: 1 / -1;
  grid-row: 1 / -1;
  place-items: center;
  color: var(--muted);
}
.image-preview {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  padding: 40px;
  place-items: center;
  background: rgb(5 18 29 / 92%);
}
.image-preview img {
  max-width: min(1100px, 94vw);
  max-height: 88vh;
  object-fit: contain;
}
.image-preview button {
  position: absolute;
  top: 20px;
  right: 24px;
  border: 0;
  color: #fff;
  background: transparent;
  cursor: pointer;
  font-size: 36px;
}
.section-nav {
  position: sticky;
  z-index: 25;
  top: 0;
  display: flex;
  gap: 4px;
  margin: 28px 0 0;
  padding: 0 8px;
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 10px 30px rgb(16 42 67 / 8%);
  backdrop-filter: blur(12px);
}
.section-nav button {
  min-width: 88px;
  min-height: 56px;
  padding: 0 18px;
  border: 0;
  border-bottom: 3px solid transparent;
  color: var(--muted);
  background: transparent;
  cursor: pointer;
  font-weight: 600;
  white-space: nowrap;
}
.section-nav button.active {
  color: var(--brand);
}
.section-nav button:hover {
  color: var(--brand);
}
.section-indicator {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 3px;
  background: var(--brand);
  pointer-events: none;
  transition:
    transform 220ms ease,
    width 220ms ease;
}
.anchor-section {
  scroll-margin-top: 76px;
}
.room-inventory {
  margin-top: 24px;
  padding: 30px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 18px 50px rgb(16 42 67 / 7%);
}
.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}
.section-heading h2,
.content-section h2 {
  margin: 0;
  font-family: var(--font-ui);
  font-size: 34px;
  font-weight: 400;
}
.section-heading > div > p:last-child {
  margin: 9px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.eyebrow {
  margin: 0 0 10px;
  color: var(--teal);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
}
.inventory-count {
  padding: 7px 11px;
  border-radius: 999px;
  color: var(--teal);
  background: var(--teal-soft);
  font-size: 11px;
  white-space: nowrap;
}
.availability-search {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) 190px 190px;
  gap: 18px;
  align-items: end;
  margin-top: 26px;
  padding: 20px;
  border-radius: 10px;
  background: #f2f7f7;
}
.availability-search :deep(.date-range input),
.guest-field select {
  min-height: 46px;
  border-color: #bcccdc;
  background: #fff;
}
.guest-field {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}
.guest-field select {
  width: 100%;
  padding: 0 12px;
  border: 1px solid #bcccdc;
  border-radius: 4px;
  color: #243b53;
}
.stay-summary {
  display: grid;
  gap: 3px;
  min-height: 46px;
  align-content: center;
  padding: 0 15px;
  border-left: 1px solid #cbd8dc;
}
.stay-summary span,
.stay-summary small {
  color: var(--muted);
  font-size: 10px;
}
.stay-summary strong {
  font-size: 18px;
}
.availability-error {
  margin: 12px 0 0;
  padding: 10px 12px;
  color: #a53c3c;
  background: #fff0f0;
  font-size: 12px;
}
.room-filters {
  display: flex;
  gap: 8px;
  margin: 20px 0 14px;
  overflow-x: auto;
}
.room-filters button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  color: #52606d;
  background: #fff;
  cursor: pointer;
  white-space: nowrap;
}
.room-filters button.active {
  border-color: var(--teal);
  color: var(--teal);
  background: var(--teal-soft);
}
.room-card-list {
  display: grid;
  gap: 14px;
}
.room-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 180px;
  gap: 22px;
  align-items: stretch;
  min-height: 190px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: 180ms ease;
}
.room-card:hover {
  border-color: #9fb3c8;
  box-shadow: 0 12px 30px rgb(16 42 67 / 9%);
  transform: translateY(-1px);
}
.room-card.selected {
  border-color: var(--teal);
  box-shadow:
    inset 4px 0 var(--teal),
    0 12px 30px rgb(16 42 67 / 8%);
}
.room-image {
  min-height: 158px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  overflow: hidden;
  color: var(--muted);
  background: #e9eef2;
  cursor: zoom-in;
}
.room-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.room-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 4px 0;
}
.room-title-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}
.room-title-line > div {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  align-items: center;
}
.room-title-line h3 {
  margin: 0;
  font-size: 21px;
}
.recommend-badge {
  padding: 4px 7px;
  border-radius: 4px;
  color: #fff;
  background: var(--teal);
  font-size: 10px;
}
.stock-badge {
  padding: 5px 8px;
  border-radius: 5px;
  color: #2f855a;
  background: #eaf7ef;
  font-size: 11px;
  white-space: nowrap;
}
.stock-badge.urgent {
  color: #c05621;
  background: #fff3e6;
}
.stock-badge.sold {
  color: #718096;
  background: #edf2f7;
}
.room-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.room-specs span {
  padding: 0 10px;
  border-right: 1px solid var(--line);
}
.room-specs span:first-child {
  padding-left: 0;
}
.room-specs span:last-child {
  border: 0;
}
.room-benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 14px;
}
.room-benefits span,
.room-facilities span {
  padding: 4px 7px;
  border: 1px solid #d8e5e2;
  border-radius: 4px;
  color: var(--teal);
  background: #f4faf9;
  font-size: 10px;
}
.room-facilities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.room-facilities span {
  border-color: var(--line);
  color: var(--muted);
  background: #fff;
}
.room-note {
  margin: auto 0 0;
  padding-top: 12px;
  color: #9fb3c8;
  font-size: 10px;
}
.room-commerce {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  flex-direction: column;
  padding-left: 18px;
  border-left: 1px solid #e4e7eb;
  text-align: right;
}
.room-commerce > span,
.room-commerce > small {
  color: var(--muted);
  font-size: 10px;
}
.room-commerce p {
  margin: 5px 0 2px;
  color: #d94c36;
}
.room-commerce p small {
  font-size: 15px;
}
.room-commerce p strong {
  font-size: 31px;
}
.room-commerce p em {
  color: var(--muted);
  font-size: 11px;
  font-style: normal;
}
.room-commerce button {
  width: 142px;
  min-height: 44px;
  margin-top: 16px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  background: var(--teal);
  cursor: pointer;
  font-weight: 700;
}
.room-commerce button:hover {
  background: #0d6975;
}
.room-commerce button:disabled {
  cursor: not-allowed;
  background: #aebbc4;
}
.room-empty {
  padding: 48px;
  color: var(--muted);
  text-align: center;
}
.detail-layout {
  margin-top: 28px;
}
.detail-content {
  display: grid;
  gap: 28px;
}
.content-section {
  padding: 32px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
}
.content-section h2 {
  margin-bottom: 22px;
}
.description {
  margin: 0;
  color: #52606d;
  line-height: 1.9;
}
.feature-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 26px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}
.feature-strip span {
  display: grid;
  gap: 4px;
  padding: 18px;
  border-right: 1px solid var(--line);
  color: var(--muted);
  font-size: 11px;
}
.feature-strip span:last-child {
  border: 0;
}
.feature-strip strong {
  color: var(--ink);
  font-size: 20px;
}
.merchant-card {
  display: grid;
  grid-template-columns: 82px 1fr;
  gap: 22px;
  padding: 24px;
  border: 1px solid #cad8df;
  border-radius: 10px;
  background: #fbfdfd;
  box-shadow: 7px 7px 0 #dcecea;
}
.merchant-mark {
  display: grid;
  width: 76px;
  height: 76px;
  border: 1px solid var(--teal);
  border-radius: 50%;
  place-items: center;
  color: var(--teal);
  background: #eef8f6;
  font-family: var(--font-ui);
  font-size: 32px;
}
.merchant-copy {
  display: grid;
  gap: 13px;
}
.merchant-title {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.merchant-title > div {
  display: grid;
  gap: 5px;
}
.merchant-title strong {
  font-size: 19px;
}
.merchant-title span,
.merchant-copy p,
.empty-copy {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.7;
}
.verified-badge {
  align-self: start;
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--teal) !important;
  background: var(--teal-soft);
  white-space: nowrap;
}
.message-merchant-button {
  width: fit-content;
  min-height: 38px;
  padding: 0 18px;
  border: 1px solid var(--ink);
  border-radius: 5px;
  color: var(--ink);
  background: #fff;
  cursor: pointer;
  font-weight: 700;
}
.facility-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.facility-grid span {
  padding: 14px;
  border-radius: 6px;
  color: #52606d;
  background: #f5f8fa;
  font-size: 12px;
}
.nearby-list {
  display: grid;
  border-top: 1px solid var(--line);
}
.nearby-list article {
  display: grid;
  grid-template-columns: 62px 1fr auto;
  gap: 18px;
  align-items: center;
  padding: 17px 4px;
  border-bottom: 1px solid var(--line);
}
.nearby-list article > span {
  color: var(--teal);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}
.nearby-list article div {
  display: grid;
  gap: 5px;
}
.nearby-list article p {
  margin: 0;
  color: var(--muted);
  font-size: 11px;
}
.nearby-list article b {
  color: #52606d;
  font-size: 11px;
}
.policy-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.policy-grid p {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 17px;
  border-radius: 6px;
  background: #f5f8fa;
}
.policy-grid span {
  color: var(--muted);
  font-size: 11px;
  line-height: 1.6;
}
.guest-reviews {
  margin-top: 38px;
  padding-top: 34px;
  border-top: 1px solid var(--line);
}
.reviews-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.reviews-heading h2 {
  margin: 0;
  font-size: 25px;
  font-weight: 600;
}
.reviews-heading h2 span {
  margin-left: 5px;
  font-weight: 400;
}
.reviews-heading > span {
  color: var(--muted);
  font-size: 12px;
}
.review-overview {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 70px;
  align-items: center;
  padding: 32px 0;
}
.review-overall > div {
  display: flex;
  align-items: baseline;
  gap: 12px;
  color: #3f8964;
}
.review-overall strong {
  font-size: 60px;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}
.review-overall > div > span {
  font-size: 27px;
  font-weight: 700;
}
.review-overall p {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.score-distribution {
  display: grid;
  gap: 9px;
}
.score-distribution-row {
  display: grid;
  grid-template-columns: 32px 1fr 30px;
  gap: 12px;
  align-items: center;
  font-size: 12px;
}
.score-distribution-row > span {
  color: #52606d;
}
.score-distribution-row b {
  color: var(--ink);
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.score-track {
  height: 5px;
  overflow: hidden;
  border-radius: 3px;
  background: #e4e7eb;
}
.score-track > span {
  display: block;
  height: 100%;
  background: #3f8964;
}
.review-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  padding: 23px 0;
  border-top: 1px solid var(--line);
}
.review-filters button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 4px;
  color: #52606d;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
}
.review-filters button.active {
  border-color: var(--ink);
  color: var(--ink);
  font-weight: 600;
}
.review-filters button:hover {
  border-color: var(--teal);
}
.guest-review {
  padding: 26px 0;
  border-bottom: 1px solid #edf1f4;
}
.guest-review > header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
}
.review-author {
  display: flex;
  gap: 12px;
  align-items: center;
}
.review-avatar {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  place-items: center;
  border-radius: 50%;
  color: var(--teal);
  background: var(--teal-soft);
  font-weight: 700;
}
.review-author > div {
  display: grid;
  gap: 5px;
}
.review-author strong {
  font-size: 15px;
}
.review-author > div > span {
  color: var(--muted);
  font-size: 12px;
}
.review-score {
  display: grid;
  gap: 4px;
  justify-items: end;
}
.review-score strong {
  font-size: 23px;
  line-height: 1;
}
.review-score :deep(.rate-star button) {
  font-size: 12px;
}
.review-content {
  margin: 18px 0;
  color: #334e68;
  font-size: 14px;
  line-height: 1.9;
  overflow-wrap: anywhere;
  white-space: pre-line;
}
.review-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.review-photos button {
  width: 128px;
  height: 112px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 5px;
  background: #e4e7eb;
  cursor: zoom-in;
}
.review-photos img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.review-date {
  display: block;
  color: var(--muted);
  font-size: 12px;
}
.merchant-review-reply {
  margin: 16px 0 0;
  padding: 18px 20px;
  border-radius: 5px;
  background: #f5f7f8;
}
.merchant-review-reply > strong {
  font-size: 14px;
}
.merchant-review-reply p {
  margin: 9px 0 0;
  color: #52606d;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-line;
  overflow-wrap: anywhere;
}
.review-empty {
  padding: 36px 16px;
  color: #52606d;
  text-align: center;
  background: #f8fafb;
  border-radius: 8px;
}
.review-empty > strong {
  font-size: 15px;
}
.review-empty p {
  margin: 9px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.review-empty button {
  margin-top: 16px;
  padding: 8px 14px;
  border: 1px solid var(--teal);
  border-radius: 4px;
  color: var(--teal);
  background: #fff;
  cursor: pointer;
}
.review-pagination {
  display: flex;
  gap: 18px;
  align-items: center;
  justify-content: center;
  padding-top: 24px;
  color: var(--muted);
  font-size: 12px;
}
.review-pagination button {
  padding: 8px 13px;
  border: 1px solid var(--line);
  border-radius: 4px;
  color: var(--ink);
  background: #fff;
  cursor: pointer;
}
.review-pagination button:disabled {
  opacity: 0.4;
  cursor: default;
}
@media (max-width: 1000px) {
  .availability-search {
    grid-template-columns: 1fr 180px;
  }
  .stay-summary {
    grid-column: 1 / -1;
    grid-template-columns: auto auto 1fr;
    gap: 10px;
    align-items: center;
    border-top: 1px solid #cbd8dc;
    border-left: 0;
    padding: 12px 0 0;
  }
  .room-card {
    grid-template-columns: 190px 1fr;
  }
  .room-commerce {
    grid-column: 2;
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 4px 14px;
    align-items: center;
    padding: 14px 0 0;
    border-top: 1px solid var(--line);
    border-left: 0;
    text-align: left;
  }
  .room-commerce p {
    grid-row: 1 / span 2;
    grid-column: 2;
  }
  .room-commerce button {
    grid-column: 2;
    grid-row: 3;
  }
}
@media (max-width: 720px) {
  .detail-main {
    width: min(100% - 28px, 1180px);
  }
  .title-row {
    align-items: flex-start;
    padding-top: 26px;
  }
  .title-row h1 {
    font-size: 36px;
  }
  .title-meta {
    align-items: flex-start;
    flex-direction: column;
  }
  .meta-divider {
    display: none;
  }
  .favorite-button {
    width: 42px;
    min-width: 42px;
    padding: 0;
  }
  .favorite-text {
    display: none;
  }
  .gallery {
    height: 360px;
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 2fr 1fr;
  }
  .gallery > button:first-child {
    grid-column: 1 / -1;
    grid-row: auto;
  }
  .gallery.double {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 1fr;
  }
  .gallery.double > button:first-child {
    grid-column: auto;
  }
  .section-nav {
    margin-inline: -14px;
    border-radius: 0;
  }
  .section-nav button {
    min-width: 74px;
    padding: 0 12px;
  }
  .room-inventory {
    padding: 20px 14px;
  }
  .section-heading {
    align-items: flex-start;
  }
  .availability-search {
    grid-template-columns: 1fr;
    padding: 15px;
  }
  .availability-search :deep(.date-range) {
    grid-template-columns: 1fr;
  }
  .availability-search :deep(.date-arrow) {
    display: none;
  }
  .stay-summary {
    grid-column: auto;
  }
  .room-card {
    grid-template-columns: 1fr;
    padding: 12px;
  }
  .room-image {
    height: 210px;
  }
  .room-title-line {
    flex-direction: column;
  }
  .room-commerce {
    grid-column: auto;
  }
  .detail-layout {
    margin-top: 28px;
  }
  .review-overview {
    grid-template-columns: 1fr;
    gap: 24px;
    padding: 25px 0;
  }
  .review-overall strong {
    font-size: 48px;
  }
  .review-overall > div > span {
    font-size: 23px;
  }
  .reviews-heading > span {
    font-size: 11px;
  }
  .review-photos button {
    width: 90px;
    height: 85px;
  }
  .review-content {
    font-size: 13px;
  }
  .content-section {
    padding: 23px 18px;
  }
  .feature-strip,
  .facility-grid,
  .policy-grid {
    grid-template-columns: 1fr;
  }
  .feature-strip span {
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }
  .merchant-card {
    grid-template-columns: 1fr;
  }
  .nearby-list article {
    grid-template-columns: 52px 1fr;
  }
  .nearby-list article b {
    grid-column: 2;
  }
}
@media (prefers-reduced-motion: reduce) {
  * {
    scroll-behavior: auto !important;
    transition: none !important;
  }
}
</style>
