<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { isNavigationFailure, NavigationFailureType, useRoute, useRouter } from 'vue-router'
import Footer from '@/components/common/Footer.vue'
import NavHeader from '@/components/common/NavHeader.vue'
import { CITIES } from '@/constants/locations'
import { getHotelPage } from '@/services/dataSource'
import { getStayNights } from '@/utils/date'
import { compareHotels, toggleCompare } from '@/utils/travelStorage'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores'
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const showSearchResults = computed(() =>
  Boolean(
    route.query.keyword ||
    route.query.city ||
    route.query.checkIn ||
    route.query.checkOut ||
    route.query.type ||
    route.query.maxPrice ||
    route.query.facility ||
    route.query.bed ||
    route.query.guests,
  ),
)
const defaults = {
  keyword: '',
  city: '',
  district: '',
  type: '',
  maxPrice: '',
  facility: '',
  bed: '',
  guests: 0,
  sort: 'score',
  checkIn: dayjs().format('YYYY-MM-DD'),
  checkOut: dayjs().add(1, 'day').format('YYYY-MM-DD'),
}
const filters = reactive({ ...defaults })
const results = ref([])
const imageErrors = ref(new Set())
const loading = ref(false)
const error = ref('')
const total = ref(0)
const pages = ref(0)
const pageNum = ref(1)
let requestId = 0
function syncQuery() {
  for (const key of Object.keys(defaults))
    filters[key] = typeof route.query[key] === 'string' ? route.query[key] : defaults[key]
  if (typeof route.query.city !== 'string') filters.city = appStore.selectedCity
  else appStore.setSelectedCity(filters.city)
  filters.guests = Number(filters.guests || 0)
  pageNum.value = Math.max(1, Number(route.query.pageNum) || 1)
}
async function refresh() {
  const id = ++requestId
  loading.value = true
  error.value = ''
  results.value = []
  total.value = 0
  pages.value = 0
  if (
    (filters.checkIn || filters.checkOut) &&
    (!getStayNights(filters.checkIn, filters.checkOut) ||
      filters.checkIn < dayjs().format('YYYY-MM-DD'))
  ) {
    error.value = '请选择有效日期，离店日期必须晚于入住日期'
    results.value = []
    total.value = 0
    pages.value = 0
    loading.value = false
    return
  }
  try {
    const data = await getHotelPage({ ...filters, pageNum: pageNum.value, pageSize: 10 })
    if (id !== requestId) return
    results.value = data.list
    total.value = data.total
    pages.value = data.pages
  } catch {
    if (id === requestId) {
      error.value = '房源加载失败，请重试'
      results.value = []
      total.value = 0
      pages.value = 0
    }
  } finally {
    if (id === requestId) loading.value = false
  }
}
async function navigateSearch(query = {}) {
  const target = { name: 'client-search', query }
  const failure = await router.replace(target)
  if (isNavigationFailure(failure, NavigationFailureType.duplicated)) {
    syncQuery()
    await refresh()
  }
}
function search(page = 1) {
  const query = Object.fromEntries(
    Object.entries({ ...filters, pageNum: page }).filter(([, v]) => v !== '' && v !== 0),
  )
  return navigateSearch(query)
}
function selectCity() {
  appStore.setSelectedCity(filters.city)
  filters.district = ''
  search()
}
function reset() {
  appStore.setSelectedCity('')
  return navigateSearch()
}
function compare(hotel) {
  if (!toggleCompare(hotel)) ElMessage.warning('最多对比3家房源，请先移出一家')
}
function detailQuery() {
  return {
    checkIn: filters.checkIn || undefined,
    checkOut: filters.checkOut || undefined,
    guests: filters.guests || undefined,
  }
}
watch(
  () => route.query,
  () => {
    syncQuery()
    refresh()
  },
)
onMounted(() => {
  syncQuery()
  refresh()
})
</script>
<template>
  <div class="hotel-page">
    <div class="hotel-header"><NavHeader /></div>
    <main>
      <section class="hotel-hero">
        <div class="hero-inner">
          <form class="hero-search-card" aria-label="酒店搜索" @submit.prevent="search()">
            <div class="hero-field destination-field">
              <label for="hotel-city">目的地</label>
              <select id="hotel-city" v-model="filters.city" @change="selectCity">
                <option value="">全国</option>
                <option v-for="city in CITIES" :key="city" :value="city">{{ city }}</option>
              </select>
            </div>
            <div class="hero-dates">
              <label class="hero-field"
                ><span>入住</span
                ><input v-model="filters.checkIn" type="date" :min="dayjs().format('YYYY-MM-DD')"
              /></label>
              <span class="date-separator">→</span>
              <label class="hero-field"
                ><span>离店</span
                ><input
                  v-model="filters.checkOut"
                  type="date"
                  :min="filters.checkIn || dayjs().format('YYYY-MM-DD')"
              /></label>
            </div>
            <label class="hero-field keyword-field">
              <span>关键词</span
              ><input v-model.trim="filters.keyword" placeholder="如位置、酒店名、品牌" />
            </label>
            <label class="hero-field guests-field">
              <span>人数</span
              ><select v-model.number="filters.guests">
                <option :value="0">不限</option>
                <option v-for="n in 6" :key="n" :value="n">{{ n }} 位住客</option>
              </select>
            </label>
            <button class="hero-submit" type="submit">
              <span aria-hidden="true">⌕</span> 搜索
            </button>
            <p v-if="error" class="hero-error" role="alert">{{ error }}</p>
          </form>
        </div>
      </section>

      <section v-if="!showSearchResults" class="hotel-recommend">
        <div class="section-title-row">
          <div>
            <h2>酒店推荐</h2>
          </div>
          <RouterLink to="/search">查看更多 <span>›</span></RouterLink>
        </div>
        <div class="city-tabs" aria-label="推荐城市">
          <span>入住城市：</span>
          <button
            v-for="city in ['', ...CITIES]"
            :key="city"
            type="button"
            :class="{ active: filters.city === city }"
            @click="city ? ((filters.city = city), selectCity()) : reset()"
          >
            {{ city || '全部' }}
          </button>
          <button type="button" class="more-city" @click="reset">更多⌄</button>
        </div>
        <div v-if="loading" class="hotel-card-grid" aria-label="正在加载酒店">
          <div v-for="n in 8" :key="n" class="hotel-skeleton"></div>
        </div>
        <div v-else-if="!results.length" class="hotel-empty">
          <strong>暂无匹配酒店</strong><span>切换城市或调整搜索条件后再试。</span
          ><button type="button" @click="reset">查看全部酒店</button>
        </div>
        <div v-else class="hotel-card-grid">
          <RouterLink
            v-for="hotel in results.slice(0, 8)"
            :key="hotel.id"
            class="hotel-card"
            :to="{ name: 'hotel-detail', params: { id: hotel.id }, query: detailQuery() }"
          >
            <div class="hotel-card-image">
              <img
                v-if="hotel.image && !imageErrors.has(hotel.image)"
                :src="hotel.image"
                :alt="hotel.name"
                loading="lazy"
                @error="imageErrors.add(hotel.image)"
              />
              <div v-else class="image-placeholder">商家暂未上传图片</div>
              <span>{{ hotel.city }}</span>
            </div>
            <div class="hotel-card-content">
              <h3 :title="hotel.name">{{ hotel.name }}</h3>
              <p>{{ hotel.district }} · {{ hotel.type || '民宿' }}</p>
              <div>
                <strong>¥{{ hotel.price }}</strong
                ><small>起</small>
              </div>
            </div>
          </RouterLink>
        </div>
      </section>

      <section v-if="showSearchResults" class="search-results-section">
        <div class="section-title-row">
          <div>
            <span class="section-eyebrow">SEARCH RESULTS</span>
            <h2>{{ filters.keyword || filters.city || '全部房源' }}</h2>
            <p aria-live="polite">{{ loading ? '正在查找可预订房源…' : `找到 ${total} 个住处` }}</p>
          </div>
          <RouterLink to="/compare">房源对比（{{ compareHotels.length }}）</RouterLink>
        </div>
        <div v-if="error" class="result-error" role="alert">
          {{ error }} <button type="button" @click="refresh">重新加载</button>
        </div>
        <div v-else-if="loading" class="hotel-empty" role="status">正在加载搜索结果…</div>
        <div v-else class="result-list">
          <article v-for="hotel in results" :key="hotel.id" class="result-card">
            <RouterLink
              class="result-image"
              :to="{ name: 'hotel-detail', params: { id: hotel.id }, query: detailQuery() }"
              ><img
                v-if="hotel.image && !imageErrors.has(hotel.image)"
                :src="hotel.image"
                :alt="hotel.name"
                @error="imageErrors.add(hotel.image)"
              />
              <div v-else class="image-placeholder">商家暂未上传图片</div></RouterLink
            >
            <div class="result-body">
              <div class="result-title">
                <div>
                  <span>{{ hotel.city }} · {{ hotel.district }}</span>
                  <h3>{{ hotel.name }}</h3>
                </div>
                <strong>{{ hotel.reviewCount > 0 ? hotel.score : '暂无评分' }}</strong>
              </div>
              <p>{{ hotel.description }}</p>
              <div class="result-foot">
                <button type="button" @click="compare(hotel)">
                  {{
                    compareHotels.some((item) => String(item.id) === String(hotel.id))
                      ? '移出对比'
                      : '加入对比'
                  }}</button
                ><b>¥{{ hotel.price }}<small> / 晚起</small></b>
                <RouterLink
                  class="result-book"
                  :to="{ name: 'hotel-detail', params: { id: hotel.id }, query: detailQuery() }"
                  >查看房型</RouterLink
                >
              </div>
            </div>
          </article>
        </div>
        <div v-if="!loading && !error && !results.length" class="hotel-empty">
          <strong>没有找到匹配房源</strong><span>尝试更换城市、日期或关键词。</span>
        </div>
        <nav v-if="pages > 1" class="pagination" aria-label="房源分页">
          <button :disabled="pageNum <= 1 || loading" @click="search(pageNum - 1)">上一页</button
          ><span>第 {{ pageNum }} / {{ pages }} 页</span
          ><button :disabled="pageNum >= pages || loading" @click="search(pageNum + 1)">
            下一页
          </button>
        </nav>
      </section>
    </main>
    <Footer />
  </div>
</template>
<style scoped>
.hotel-page {
  min-height: 100vh;
  background: #fff;
  color: #192b38;
}
.hotel-hero {
  min-height: 474px;
  background-position: center;
  background-size: cover;
}
.hero-inner {
  position: relative;
  width: min(100% - 48px, 1240px);
  height: 474px;
  margin: auto;
}
.hero-search-card {
  position: absolute;
  top: 52px;
  left: 0;
  display: grid;
  grid-template-columns: 1.2fr 1.8fr 1.25fr 1fr;
  gap: 18px 12px;
  width: min(614px, 100%);
  padding: 38px 40px 34px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 12px 40px rgb(0 0 0 / 18%);
}
.hero-field {
  display: grid;
  gap: 8px;
  color: #3f4c57;
  font-size: 13px;
}
.hero-field span,
.hero-field label {
  color: #65727e;
  font-size: 12px;
}
.hero-field input,
.hero-field select {
  width: 100%;
  min-width: 0;
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d3d7da;
  border-radius: 3px;
  color: #283844;
  background: #fff;
  font-size: 15px;
  box-sizing: border-box;
}
.destination-field {
  grid-column: 1/-1;
  grid-template-columns: 70px 1fr;
  align-items: center;
}
.destination-field label {
  font-size: 16px;
  color: #263640;
}
.hero-dates {
  grid-column: 1/3;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 8px;
  align-items: end;
}
.date-separator {
  padding-bottom: 12px;
  color: #82909a;
}
.keyword-field {
  grid-column: 1/2;
}
.guests-field {
  grid-column: 2/3;
}
.hero-submit {
  grid-column: 1/-1;
  height: 58px;
  border: 0;
  border-radius: 3px;
  color: #fff;
  background: #ff7200;
  font-size: 20px;
  font-weight: 700;
  cursor: pointer;
}
.hero-submit span {
  font-size: 28px;
  vertical-align: -2px;
}
.hero-error {
  grid-column: 1/-1;
  margin: 0;
  color: #c54a2e;
  font-size: 12px;
}
.hero-copy {
  position: absolute;
  right: 4%;
  top: 150px;
  color: #fff;
  text-shadow: 0 2px 18px rgb(0 0 0 / 36%);
}
.hero-copy span {
  font-size: 13px;
  letter-spacing: 0.15em;
}
.hero-copy h1 {
  margin: 18px 0;
  font-size: 46px;
  line-height: 1.25;
  letter-spacing: 0.02em;
}
.hero-copy p {
  font-size: 16px;
}
.hotel-recommend,
.search-results-section {
  width: min(100% - 48px, 1240px);
  margin: auto;
  padding: 32px 0 58px;
}
.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
  margin-bottom: 20px;
}
.section-title-row h2 {
  margin: 7px 0 0;
  font-size: 30px;
  letter-spacing: -0.03em;
}
.section-title-row p {
  margin: 8px 0 0;
  color: #8b98a2;
  font-size: 13px;
}
.section-title-row > a {
  color: #71808b;
  text-decoration: none;
  font-size: 13px;
}
.section-title-row > a span {
  font-size: 22px;
  vertical-align: -2px;
}
.section-eyebrow {
  color: #ff7200;
  font-size: 10px;
  letter-spacing: 0.18em;
  font-weight: 700;
}
.city-tabs {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin: 0 0 25px;
  color: #71808b;
  font-size: 14px;
}
.city-tabs button {
  padding: 8px 16px;
  border: 0;
  border-radius: 20px;
  color: #263640;
  background: transparent;
  cursor: pointer;
  font-size: 15px;
}
.city-tabs button.active {
  color: #fff;
  background: #ff7200;
}
.city-tabs .more-city {
  padding: 8px 3px;
  color: #596873;
}
.hotel-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px 20px;
}
.hotel-card {
  min-width: 0;
  color: inherit;
  text-decoration: none;
}
.hotel-card-image {
  position: relative;
  aspect-ratio: 1.58;
  overflow: hidden;
  border-radius: 15px;
}
.hotel-card-image img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.25s;
}
.image-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  min-height: 140px;
  color: #647681;
  background: #eef2f3;
  font-size: 13px;
}
.hotel-card:hover img {
  transform: scale(1.04);
}
.hotel-card-image > span {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 12px;
  border-radius: 15px;
  color: #fff;
  background: rgb(22 46 64 / 76%);
  font-size: 11px;
}
.hotel-card-content {
  padding: 13px 5px 0;
}
.hotel-card-content h3 {
  overflow: hidden;
  margin: 0;
  font-size: 16px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.hotel-card-content p {
  overflow: hidden;
  margin: 7px 0;
  color: #89949c;
  font-size: 11px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.hotel-card-content div {
  display: flex;
  align-items: baseline;
  gap: 3px;
  color: #ff604e;
}
.hotel-card-content strong {
  font-size: 23px;
}
.hotel-card-content small {
  font-size: 11px;
}
.hotel-skeleton {
  aspect-ratio: 1.58;
  border-radius: 15px;
  background: linear-gradient(90deg, #eef2f3 25%, #f8f9fa 37%, #eef2f3 63%);
  background-size: 400% 100%;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
  to {
    background-position: -400% 0;
  }
}
.hotel-empty {
  display: grid;
  place-items: center;
  gap: 10px;
  padding: 60px 20px;
  background: #f7f9fa;
  color: #536572;
  text-align: center;
}
.hotel-empty span {
  color: #8a969e;
  font-size: 13px;
}
.hotel-empty button,
.result-error button {
  padding: 9px 17px;
  border: 1px solid #ff7200;
  border-radius: 3px;
  color: #ff7200;
  background: #fff;
  cursor: pointer;
}
.search-results-section {
  border-top: 1px solid #edf0f1;
  padding-top: 44px;
}
.result-list {
  display: grid;
  gap: 14px;
}
.result-card {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 190px;
  overflow: hidden;
  border: 1px solid #e5e8e9;
  background: #fff;
}
.result-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.result-body {
  display: grid;
  padding: 19px;
}
.result-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.result-title span {
  color: #8b98a2;
  font-size: 11px;
}
.result-title h3 {
  margin: 5px 0 0;
  font-size: 20px;
}
.result-title > strong {
  color: #c77b1d;
  background: #fff6df;
  padding: 7px 10px;
  height: fit-content;
}
.result-body > p {
  margin: 12px 0;
  color: #647681;
  font-size: 13px;
  line-height: 1.7;
}
.result-foot {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin-top: auto;
}
.result-foot button {
  padding: 7px 11px;
  border: 1px solid #cbd7dc;
  border-radius: 3px;
  color: #497288;
  background: #fff;
  cursor: pointer;
}
.result-foot > b {
  color: #ff604e;
  font-size: 23px;
}
.result-foot small {
  font-size: 11px;
  font-weight: 400;
}
.result-error {
  padding: 28px;
  background: #fff4ee;
  color: #b24f35;
  text-align: center;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding: 28px;
}
.pagination button {
  padding: 8px 13px;
  border: 1px solid #cbd7dc;
  background: #fff;
  color: #397aab;
  cursor: pointer;
}
.pagination button:disabled {
  opacity: 0.4;
}
@media (max-width: 900px) {
  .hero-copy {
    right: 2%;
    top: auto;
    bottom: 38px;
  }
  .hero-copy h1 {
    font-size: 34px;
  }
  .hero-search-card {
    left: 20px;
    width: min(560px, calc(100% - 40px));
  }
  .hotel-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 620px) {
  .hotel-hero,
  .hero-inner {
    min-height: 600px;
    height: 600px;
  }
  .hero-search-card {
    top: 25px;
    left: 0;
    width: 100%;
    padding: 22px 18px;
    box-sizing: border-box;
    grid-template-columns: 1fr 1fr;
    gap: 13px 9px;
  }
  .destination-field {
    grid-column: 1/-1;
    grid-template-columns: 58px 1fr;
  }
  .hero-dates {
    grid-column: 1/-1;
  }
  .keyword-field,
  .guests-field {
    grid-column: auto;
  }
  .hero-submit {
    height: 50px;
    font-size: 17px;
  }
  .hero-copy {
    right: 12px;
    bottom: 25px;
    left: 12px;
  }
  .hero-copy h1 {
    font-size: 30px;
    margin: 7px 0;
  }
  .hero-copy p {
    font-size: 13px;
  }
  .hotel-recommend,
  .search-results-section {
    width: calc(100% - 32px);
    padding-top: 26px;
  }
  .section-title-row h2 {
    font-size: 25px;
  }
  .city-tabs {
    gap: 8px;
  }
  .city-tabs button {
    padding: 7px 11px;
    font-size: 13px;
  }
  .hotel-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px 12px;
  }
  .hotel-card-image {
    border-radius: 10px;
  }
  .hotel-card-content {
    padding-top: 9px;
  }
  .hotel-card-content h3 {
    font-size: 14px;
  }
  .hotel-card-content strong {
    font-size: 19px;
  }
  .result-card {
    grid-template-columns: 1fr;
  }
  .result-image {
    aspect-ratio: 1.7;
  }
  .result-body {
    padding: 15px;
  }
}
</style>
