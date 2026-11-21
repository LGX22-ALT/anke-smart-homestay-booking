<script setup>
import { ArrowRight, Location } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import Footer from '@/components/common/Footer.vue'
import NavHeader from '@/components/common/NavHeader.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { listHotels } from '@/services/dataSource'

import AiStayAssistant from '@/components/business/AiStayAssistant.vue'
import { attractions, guides, travelCities } from '@/constants/travel'
import { useAppStore } from '@/stores'
const appStore = useAppStore()
const router = useRouter()
const stays = ref([])
const loading = ref(true)
const loadError = ref(false)
const expandedAttractions = ref(false)
const selectedAttraction = ref(null)
const dialogVisible = ref(false)
const cityAttractions = computed(() =>
  attractions.filter((item) => !selectedCity.value || item.city === selectedCity.value),
)
const visibleGuides = computed(() =>
  guides.filter((item) => !selectedCity.value || item.city === selectedCity.value).slice(0, 3),
)
const selectedCity = computed({
  get: () => appStore.selectedCity,
  set: (city) => appStore.setSelectedCity(city),
})
let requestId = 0

const visibleAttractions = computed(() =>
  expandedAttractions.value ? cityAttractions.value : cityAttractions.value.slice(0, 4),
)

async function loadStays() {
  const currentRequest = ++requestId
  loading.value = true
  loadError.value = false
  try {
    const rows = await listHotels({ pageNum: 1, pageSize: 8, city: selectedCity.value })
    if (currentRequest === requestId) stays.value = rows.slice(0, 8)
  } catch {
    if (currentRequest === requestId) loadError.value = true
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

function handleImageError(event) {
  event.currentTarget.hidden = true
}

function openAttraction(attraction) {
  selectedAttraction.value = attraction
  dialogVisible.value = true
}

function exploreNearby() {
  dialogVisible.value = false
  router.push({ name: 'client-search', query: { city: selectedAttraction.value.city } })
}

function priceLabel(price) {
  const amount = Number(price)
  return Number.isFinite(amount) && amount > 0
    ? amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
    : null
}

watch(selectedCity, () => {
  expandedAttractions.value = false
  loadStays()
})
onMounted(loadStays)
</script>

<template>
  <div class="home-page">
    <NavHeader />
    <main class="home-main">
      <AiStayAssistant />
      <div class="portal-shortcuts">
        <RouterLink to="/search">酒店民宿 <span>找一间好住处 →</span></RouterLink
        ><RouterLink to="/attractions">景点探索 <span>发现城市新风景 →</span></RouterLink
        ><RouterLink to="/guides">旅行攻略 <span>安排轻松行程 →</span></RouterLink
        ><RouterLink to="/history">最近浏览 <span>继续上次的发现 →</span></RouterLink>
      </div>
      <section
        id="featured"
        class="recommend-section"
        aria-labelledby="stays-title"
        :aria-busy="loading"
      >
        <header class="section-heading">
          <div>
            <h2 id="stays-title">{{ selectedCity || '目的地' }}精选住宿</h2>
            <p>选一家喜欢的住处，开始下一段旅程</p>
          </div>
          <RouterLink
            class="section-action"
            :to="{ name: 'client-search', query: { city: selectedCity || undefined } }"
            >查看更多 <ArrowRight
          /></RouterLink>
        </header>
        <div class="city-tabs" aria-label="推荐酒店城市">
          <button
            v-for="city in ['', ...travelCities]"
            :key="city"
            :class="{ active: selectedCity === city }"
            :aria-pressed="selectedCity === city"
            @click="selectedCity = city"
          >
            {{ city || '全部' }}
          </button>
        </div>
        <div v-if="loading" class="recommend-grid" role="status" aria-label="正在加载推荐酒店">
          <div v-for="index in 8" :key="index" class="loading-card">
            <div></div>
            <span></span>
          </div>
        </div>
        <div v-else-if="loadError" class="section-empty" role="status">
          <strong>推荐酒店暂时无法加载</strong>
          <p>请稍后重试，或进入房源列表查看。</p>
          <button type="button" @click="loadStays">重新加载</button>
        </div>
        <div v-else-if="!stays.length" class="section-empty">
          <strong>新的好住处正在准备中</strong>
          <p>
            {{
              selectedCity
                ? '该城市暂无上架房源，可切换其他城市查看。'
                : '商家房源上架后，会在这里展示。'
            }}
          </p>
        </div>
        <div v-else class="recommend-grid">
          <RouterLink
            v-for="stay in stays"
            :key="stay.id"
            class="recommend-card stay-card"
            :to="{ name: 'hotel-detail', params: { id: stay.id } }"
          >
            <div class="card-image-wrap">
              <span class="image-placeholder" aria-hidden="true">{{ stay.name }}</span>
              <img :src="stay.image" :alt="stay.name" loading="lazy" @error="handleImageError" />
            </div>
            <div class="card-caption">
              <h3 :title="stay.name">{{ stay.name }}</h3>
              <p v-if="priceLabel(stay.price)" class="card-price">
                <small>¥</small><strong>{{ priceLabel(stay.price) }}</strong
                ><small>起 / 晚</small>
              </p>
              <span v-else class="price-pending">价格待确认</span>
            </div>
            <p class="card-detail">
              {{ stay.city }}<span v-if="stay.district"> · {{ stay.district }}</span>
            </p>
            <div class="stay-meta">
              <template v-if="Number(stay.score) > 0">
                <span class="score">{{ Number(stay.score).toFixed(1) }}</span>
                <span>住客评分</span>
              </template>
              <span v-else>暂无评分</span>
              <span>{{ stay.type }}</span>
            </div>
            <div class="stay-facilities">
              <span v-for="facility in stay.facilities?.slice(0, 2)" :key="facility">{{
                facility
              }}</span>
            </div>
            <div v-if="stay.merchant" class="stay-host">
              <UserAvatar
                :src="stay.merchant.avatar || ''"
                :name="stay.merchant.name || stay.merchant.companyName || '民宿商家'"
              />
              <span>{{ stay.merchant.name || stay.merchant.companyName || '民宿商家' }}</span
              ><small>房东</small>
            </div>
          </RouterLink>
        </div>
      </section>

      <section
        id="destinations"
        class="recommend-section attractions-section"
        aria-labelledby="attractions-title"
      >
        <header class="section-heading">
          <div>
            <h2 id="attractions-title">玩景点</h2>
            <p>
              探索{{ selectedCity || '各城市' }}的
              {{ cityAttractions.length }} 处风景，连接下一段旅程
            </p>
          </div>
          <button
            class="section-action"
            type="button"
            :aria-expanded="expandedAttractions"
            @click="expandedAttractions = !expandedAttractions"
          >
            {{ expandedAttractions ? '收起景点' : '查看更多' }}
            <ArrowRight :class="{ expanded: expandedAttractions }" />
          </button>
        </header>
        <div class="recommend-grid">
          <button
            v-for="attraction in visibleAttractions"
            :key="attraction.name"
            class="recommend-card attraction-card"
            type="button"
            @click="openAttraction(attraction)"
          >
            <div class="card-image-wrap">
              <span class="image-placeholder" aria-hidden="true">{{ attraction.name }}</span>
              <img
                v-if="attraction.image"
                :src="attraction.image"
                :alt="attraction.name"
                loading="lazy"
                @error="handleImageError"
              />
              <span class="city-badge">{{ attraction.city }}</span>
            </div>
            <div class="card-caption">
              <h3>{{ attraction.name }}</h3>
              <span class="attraction-tag">{{ attraction.tag }}</span>
            </div>
            <p class="card-detail">{{ attraction.district }}</p>
          </button>
        </div>
      </section>
      <section v-if="visibleGuides.length" class="attractions-section">
        <header class="section-heading">
          <div>
            <h2>攻略玩法</h2>
            <p>先看路线，再选住处</p>
          </div>
          <RouterLink to="/guides" class="section-action">全部攻略 <ArrowRight /></RouterLink>
        </header>
        <div class="guide-grid">
          <RouterLink v-for="guide in visibleGuides" :key="guide.id" :to="'/guides/' + guide.id"
            ><img :src="guide.image" :alt="guide.title" loading="lazy" />
            <div>
              <small>{{ guide.theme }}</small>
              <h3>{{ guide.title }}</h3>
              <p>{{ guide.summary }}</p>
            </div></RouterLink
          >
        </div>
      </section>
      <div class="service-band">
        <span>日期房态实时查询</span><span>入住前可私信商家</span><span>订单进度随时查看</span
        ><RouterLink to="/help">预订帮助 →</RouterLink>
      </div>
    </main>

    <el-dialog
      v-model="dialogVisible"
      :title="selectedAttraction?.name || '景点介绍'"
      width="min(620px, 94vw)"
    >
      <article v-if="selectedAttraction" class="attraction-dialog">
        <div class="dialog-photo">
          <span class="image-placeholder">{{ selectedAttraction.name }}</span
          ><img
            v-if="selectedAttraction.image"
            :src="selectedAttraction.image"
            :alt="selectedAttraction.name"
            @error="handleImageError"
          />
        </div>
        <p class="dialog-location"><Location />{{ selectedAttraction.district }}</p>
        <p class="dialog-description">{{ selectedAttraction.description }}</p>
        <div class="dialog-actions">
          <a
            v-if="selectedAttraction.image"
            :href="selectedAttraction.source"
            target="_blank"
            rel="noreferrer"
            >图片：{{ selectedAttraction.author }}</a
          ><button type="button" @click="exploreNearby">
            查看{{ selectedAttraction.city }}住宿 <ArrowRight />
          </button>
        </div>
        <p v-if="selectedAttraction.image" class="photo-credit">
          <a :href="selectedAttraction.licenseUrl" target="_blank" rel="noreferrer">{{
            selectedAttraction.license
          }}</a>
          · 图片已缩放压缩，展示时裁切
        </p>
      </article>
    </el-dialog>
    <Footer />
  </div>
</template>

<style scoped>
.stay-host {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  color: var(--ink);
}
.stay-host :deep(.account-avatar) {
  width: 26px;
  height: 26px;
}
.stay-host > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stay-host small {
  margin-left: auto;
  flex-shrink: 0;
  color: var(--muted);
  font-size: 11px;
}

.stay-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: #798496;
  font-size: 11px;
  margin: 12px 0;
}
.stay-meta .score {
  white-space: nowrap;
  flex-shrink: 0;
  background: #ede8fa;
  color: #6945b0;
  padding: 4px 7px;
  border-radius: 4px;
  font-weight: 700;
}
.stay-facilities {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  font-size: 10px;
  color: #637286;
}
.stay-facilities span {
  padding: 3px 6px;
  border: 1px solid #e8ebf0;
  border-radius: 3px;
}
.home-page .home-main {
  padding-top: 28px;
}
.home-page {
  background: #f6f7fa;
}
.home-page .recommend-section {
  background: white;
  padding: 26px;
  border: 1px solid #edf0f5;
  border-radius: 14px;
}
.home-page .section-heading h2 {
  font-size: 25px;
  letter-spacing: 0;
}
.home-page .recommend-grid {
  gap: 26px 18px;
}
.home-page .card-image-wrap {
  border-radius: 9px;
}
.home-page .card-caption {
  flex-wrap: wrap;
  gap: 8px;
}
.home-page .card-caption h3 {
  flex-basis: 100%;
  font-size: 15px;
}
.home-page .city-tabs button.active {
  background: #6844b6;
}
.home-page .card-price {
  color: #db6532;
}
.home-page .ai-stay {
  margin: 0 0 30px;
  width: 100%;
}
.home-page .portal-shortcuts {
  margin-bottom: 24px;
}
.home-page .portal-shortcuts a {
  background: white;
  border: 1px solid #edf0f5;
  border-radius: 9px;
  padding: 18px 22px;
  font-size: 15px;
}
button:focus-visible,
a:focus-visible,
input:focus-visible,
select:focus-visible {
  outline: 3px solid #ac95e3;
  outline-offset: 3px;
}
@media (max-width: 760px) {
  .home-page .recommend-section {
    padding: 18px 12px;
  }
  .home-page .portal-shortcuts a {
    padding: 14px;
  }
  .home-page .section-heading h2 {
    font-size: 21px;
  }
}
.portal-shortcuts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 42px;
}
.portal-shortcuts a {
  border-bottom: 2px solid #e6eff7;
  padding: 5px 0 20px;
  text-decoration: none;
  color: #173754;
  font-weight: 700;
}
.portal-shortcuts span {
  display: block;
  margin-top: 8px;
  color: #8295a5;
  font-size: 12px;
  font-weight: 400;
}
.city-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: -8px 0 24px;
}
.city-tabs button {
  padding: 7px 16px;
  border: 0;
  border-radius: 20px;
  background: #f3f6f9;
  color: #637a8e;
  cursor: pointer;
}
.city-tabs button.active {
  background: #4097ee;
  color: white;
}
.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}
.guide-grid a {
  color: inherit;
  text-decoration: none;
  min-width: 0;
  border: 1px solid #e6edf3;
  border-radius: 14px;
  overflow: hidden;
}
.guide-grid img {
  width: 100%;
  height: 170px;
  object-fit: cover;
}
.guide-grid a > div {
  padding: 18px;
}
.guide-grid small {
  color: var(--brand);
}
.guide-grid h3 {
  font-size: 17px;
  line-height: 1.5;
  margin: 10px 0;
}
.guide-grid p {
  font-size: 12px;
  color: #8295a5;
  line-height: 1.8;
}
.service-band {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 44px;
  padding: 25px;
  background: #f5f9fc;
  color: var(--muted);
  font-size: 13px;
}
.service-band a {
  color: #2582cc;
}
@media (max-width: 650px) {
  .portal-shortcuts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .guide-grid {
    grid-template-columns: 1fr;
  }
  .guide-grid img {
    height: 180px;
  }
}

.home-page {
  min-height: 100vh;
  color: #1c2833;
  background: #fff;
}
.home-main {
  width: min(100% - 48px, 1220px);
  margin: 0 auto;
  padding: 54px 0 90px;
}
.recommend-section {
  scroll-margin-top: 24px;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}
.section-heading h1,
.section-heading h2 {
  margin: 0;
  color: #152433;
  font-family: var(--font-ui);
  font-size: 34px;
  font-weight: 750;
  line-height: 1.3;
  letter-spacing: -0.04em;
}
.section-heading p {
  margin: 9px 0 0;
  color: #86919c;
  font-size: 13px;
}
.section-action {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 5px;
  padding: 8px 0 8px 12px;
  border: 0;
  color: #88929a;
  background: transparent;
  text-decoration: none;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
.section-action svg {
  width: 15px;
  height: 15px;
  transition: transform 180ms ease;
}
.section-action:hover {
  color: var(--brand);
}
.section-action svg.expanded {
  transform: rotate(-90deg);
}
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px 22px;
}
.recommend-card {
  display: block;
  min-width: 0;
  padding: 0;
  border: 0;
  color: inherit;
  background: transparent;
  text-decoration: none;
  text-align: left;
  cursor: pointer;
  font: inherit;
}
.card-image-wrap {
  position: relative;
  aspect-ratio: 1.52;
  overflow: hidden;
  border-radius: 16px;
  background: #e7eff0;
  isolation: isolate;
}
.image-placeholder {
  position: absolute;
  inset: 0;
  display: grid;
  padding: 20px;
  place-items: center;
  color: #627d88;
  background: #e7eff0;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}
.card-image-wrap img {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 260ms ease;
}
.card-image-wrap img[hidden],
.dialog-photo img[hidden] {
  display: none;
}
.recommend-card:hover img {
  transform: scale(1.045);
}
.recommend-card:hover h3 {
  color: var(--brand);
}
.recommend-card:focus-visible,
.section-action:focus-visible {
  outline: 3px solid #4097ee;
  outline-offset: 6px;
  border-radius: 6px;
}
.card-caption {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin: 14px 3px 0;
}
.card-caption h3 {
  min-width: 0;
  overflow: hidden;
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.55;
}
.card-price {
  display: inline-flex;
  align-items: baseline;
  flex-shrink: 0;
  gap: 3px;
  margin: 0;
  color: #ff6555;
  white-space: nowrap;
}
.card-price strong {
  font-family: var(--font-ui);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.04em;
}
.card-price small {
  font-size: 10px;
}
.price-pending {
  flex-shrink: 0;
  color: #86919c;
  font-size: 11px;
}
.card-detail {
  margin: 5px 3px 0;
  overflow: hidden;
  color: #8a959e;
  font-size: 11px;
  line-height: 1.7;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.attractions-section {
  margin-top: 58px;
}
.city-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  display: inline-flex;
  padding: 6px 12px;
  border: 1px solid rgb(255 255 255 / 25%);
  border-radius: 999px;
  color: #fff;
  background: rgb(16 42 67 / 64%);
  font-size: 11px;
}
.attraction-tag {
  flex-shrink: 0;
  color: var(--brand);
  font-size: 10px;
}
.section-empty {
  padding: 50px 20px;
  border-radius: 12px;
  color: #52606d;
  background: #f6f9fa;
  text-align: center;
}
.section-empty p {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.section-empty button {
  margin-top: 18px;
  padding: 9px 18px;
  border: 1px solid var(--brand);
  border-radius: 6px;
  color: var(--brand);
  background: #fff;
  cursor: pointer;
}
.loading-card > div {
  aspect-ratio: 1.52;
  border-radius: 16px;
  background: #edf2f4;
}
.loading-card > span {
  display: block;
  width: 70%;
  height: 18px;
  margin-top: 16px;
  border-radius: 4px;
  background: #edf2f4;
}
.dialog-photo {
  position: relative;
  aspect-ratio: 1.8;
  overflow: hidden;
  border-radius: 10px;
}
.dialog-photo img {
  position: relative;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.dialog-location {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 18px 0 10px;
  color: var(--brand);
  font-size: 12px;
}
.dialog-location svg {
  width: 15px;
}
.dialog-description {
  margin: 0;
  color: #52606d;
  font-size: 14px;
  line-height: 1.9;
}
.dialog-actions {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  align-items: center;
  margin-top: 24px;
}
.photo-credit {
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 10px;
}
.photo-credit a {
  color: inherit;
}
.dialog-actions a {
  color: var(--muted);
  font-size: 11px;
}
.dialog-actions button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 16px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  background: var(--brand);
  cursor: pointer;
}
.dialog-actions svg {
  width: 15px;
}
@media (max-width: 1000px) {
  .recommend-grid {
    gap: 24px 16px;
  }
  .card-caption {
    flex-wrap: wrap;
    gap: 2px;
  }
  .card-caption h3 {
    flex-basis: 100%;
  }
}
@media (max-width: 760px) {
  .home-main {
    width: calc(100% - 32px);
    padding-top: 30px;
  }
  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px 14px;
  }
  .section-heading {
    margin-bottom: 20px;
    align-items: baseline;
  }
  .section-heading h1,
  .section-heading h2 {
    font-size: 27px;
  }
  .section-heading p {
    max-width: 210px;
    font-size: 11px;
    line-height: 1.6;
  }
  .section-action {
    font-size: 12px;
  }
  .card-image-wrap {
    border-radius: 11px;
  }
  .card-caption {
    margin: 9px 1px 0;
  }
  .card-caption h3 {
    font-size: 14px;
  }
  .card-price strong {
    font-size: 22px;
  }
  .card-detail {
    margin-left: 1px;
    font-size: 10px;
  }
  .city-badge {
    top: 8px;
    left: 8px;
    padding: 4px 9px;
    font-size: 10px;
  }
  .attractions-section {
    margin-top: 40px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .recommend-card img,
  .section-action svg {
    transition: none;
  }
}
</style>
