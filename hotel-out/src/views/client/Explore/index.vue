<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import NavHeader from '@/components/common/NavHeader.vue'
import Footer from '@/components/common/Footer.vue'
import { attractions, guides, travelCities } from '@/constants/travel'
import { useAppStore } from '@/stores'
import { browseHistory, clearHistory, compareHotels, toggleCompare } from '@/utils/travelStorage'
const route = useRoute()
const keyword = ref('')
const category = ref('')
const appStore = useAppStore()
const city = computed({
  get: () => appStore.selectedCity,
  set: (value) => appStore.setSelectedCity(value),
})
const themes = computed(() => [...new Set(attractions.map((item) => item.tag))])
const pageType = computed(() => route.meta.exploreType)
const attraction = computed(() => attractions.find((item) => item.id === route.params.id))
const guide = computed(() => guides.find((item) => item.id === route.params.id))
const filtered = computed(() =>
  attractions.filter(
    (item) =>
      (!city.value || item.city === city.value) &&
      (!category.value || item.tag === category.value) &&
      (item.city + item.name + item.district + item.description).includes(keyword.value.trim()),
  ),
)
const titles = {
  attractions: '景点探索',
  guides: '旅行攻略',
  history: '最近浏览',
  compare: '房源对比',
  help: '预订帮助',
}
const faqs = [
  [
    '如何预订住宿？',
    '搜索目的地并选择日期，在房源详情中查看可订房型，填写入住信息并提交订单。订单进度可在“我的订单”中查看，支付服务状态以确认页面提示为准。',
  ],
  [
    '为什么提示没有房间？',
    '房态按日期计算，连续入住的每一晚都需要有库存。可调整日期、人数或选择其他房型。',
  ],
  [
    '如何联系商家？',
    '进入房源详情，在商家信息处点击私信商家。发送消息前需要登录住客账号，商家可在后台回复。',
  ],
  [
    '如何取消订单或申请退款？',
    '登录后进入我的订单，按当前订单状态使用取消或退款操作。是否允许操作以订单页面与房源政策为准。',
  ],
  [
    '景点可以购买门票吗？',
    '当前提供景点介绍、攻略和住宿查询，尚未接入门票销售。出游前请通过景区官方渠道确认开放安排和购票规则。',
  ],
  [
    '支付和充值如何使用？',
    '在线支付与账户充值暂未开放。当前支付流程为沙箱验证，不产生实际扣款或资金退款，请勿通过非官方渠道转账。',
  ],
  [
    '浏览记录与对比保存在哪里？',
    '记录仅保存在当前浏览器，最多保存20条浏览记录和3家对比房源。可在浏览记录页清空，清除浏览器数据后也会删除。',
  ],
]
</script>
<template>
  <div class="explore-page">
    <NavHeader />
    <main class="explore-main">
      <div class="breadcrumbs">
        <RouterLink to="/">首页</RouterLink><span>/</span
        ><RouterLink
          :to="
            pageType === 'guides'
              ? '/guides'
              : pageType === 'attractions'
                ? '/attractions'
                : route.path
          "
          >{{ titles[pageType] }}</RouterLink
        ><template v-if="route.params.id"><span>/</span><span>详情</span></template>
      </div>
      <template v-if="pageType === 'attractions' && !route.params.id">
        <header class="page-heading">
          <span>发现{{ city || '目的地' }}</span>
          <h1>让风景，成为出发的理由</h1>
          <p>城市地标、岭南文化与亲子度假，从这里开始探索</p>
        </header>
        <form class="explore-filter" @submit.prevent>
          <select v-model="city" aria-label="景点城市">
            <option value="">全部城市</option>
            <option v-for="name in travelCities" :key="name">{{ name }}</option>
          </select>
          <input v-model="keyword" aria-label="搜索景点" placeholder="搜索景点名称、区域" /><select
            v-model="category"
            aria-label="景点主题"
          >
            <option value="">全部主题</option>
            <option v-for="tag in themes" :key="tag">{{ tag }}</option>
          </select>
        </form>
        <div class="explore-grid">
          <RouterLink
            v-for="item in filtered"
            :key="item.id"
            :to="'/attractions/' + item.id"
            class="explore-card"
            ><img v-if="item.image" :src="item.image" :alt="item.name" loading="lazy" />
            <div v-else class="destination-placeholder">
              {{ item.city }}<strong>{{ item.name }}</strong>
            </div>
            <div>
              <small>{{ item.city }} · {{ item.tag }}</small>
              <h2>{{ item.name }}</h2>
              <p>{{ item.district }}</p>
              <span>查看景点 →</span>
            </div></RouterLink
          >
        </div>
        <p v-if="!filtered.length" class="empty">没有匹配景点，试试其他名称或主题。</p>
      </template>
      <article v-else-if="pageType === 'attractions' && attraction" class="detail">
        <img
          v-if="attraction.image"
          class="detail-cover"
          :src="attraction.image"
          :alt="attraction.name"
        />
        <div class="detail-body">
          <small>{{ attraction.city }} · {{ attraction.tag }}</small>
          <h1>{{ attraction.name }}</h1>
          <p>{{ attraction.district }}</p>
          <h2>景点介绍</h2>
          <p>{{ attraction.description }}</p>
          <p class="notice">出发前请查询景区官方开放安排。本平台暂不销售景点门票。</p>
          <RouterLink
            class="action"
            :to="{ name: 'client-search', query: { city: attraction.city } }"
            >查找{{ attraction.city }}住宿 →</RouterLink
          >
          <p v-if="attraction.image" class="credit">
            图片：<a :href="attraction.source" target="_blank" rel="noreferrer">{{
              attraction.author
            }}</a>
            ·
            <a :href="attraction.licenseUrl" target="_blank" rel="noreferrer">{{
              attraction.license
            }}</a>
            · 已缩放压缩，展示时裁切
          </p>
        </div>
      </article>
      <template v-else-if="pageType === 'guides' && !route.params.id"
        ><header class="page-heading">
          <span>旅行灵感</span>
          <h1>把喜欢的地方，排进周末</h1>
          <p>实用路线建议，连接景点探索与住宿预订</p>
        </header>
        <div class="explore-grid">
          <RouterLink
            v-for="item in guides"
            :key="item.id"
            :to="'/guides/' + item.id"
            class="explore-card"
            ><img :src="item.image" :alt="item.title" />
            <div>
              <small>{{ item.city }} · {{ item.theme }}</small>
              <h2>{{ item.title }}</h2>
              <p>{{ item.summary }}</p>
              <span>阅读攻略 →</span>
            </div></RouterLink
          >
        </div></template
      >
      <article v-else-if="pageType === 'guides' && guide" class="detail">
        <img class="detail-cover" :src="guide.image" :alt="guide.title" />
        <div class="detail-body">
          <small>{{ guide.city }} · {{ guide.theme }}</small>
          <h1>{{ guide.title }}</h1>
          <p>{{ guide.summary }}</p>
          <section v-for="(day, index) in guide.days" :key="index" class="itinerary">
            <h2>第 {{ index + 1 }} 天</h2>
            <ol>
              <li v-for="stop in day" :key="stop">{{ stop }}</li>
            </ol>
          </section>
          <p class="notice">{{ guide.tips }}</p>
          <RouterLink class="action" :to="{ name: 'client-search', query: { city: guide.city } }"
            >选择这次旅行的住宿 →</RouterLink
          ><RouterLink class="text-link" to="/attractions">查看相关景点与图片来源</RouterLink>
        </div>
      </article>
      <template v-else-if="pageType === 'history'">
        <header class="page-heading">
          <span>继续发现</span>
          <h1>最近浏览</h1>
          <p>仅保存在当前浏览器，最多保留20条房源记录。</p>
          <button v-if="browseHistory.length" class="text-button" @click="clearHistory">
            清空浏览记录
          </button>
        </header>
        <div class="explore-grid">
          <RouterLink
            v-for="item in browseHistory"
            :key="item.id"
            :to="'/hotel/' + item.id"
            class="explore-card"
            ><img :src="item.image" :alt="item.name" />
            <div>
              <small>{{ item.city }} · {{ item.district }}</small>
              <h2>{{ item.name }}</h2>
              <strong class="price">¥{{ item.price }} 起</strong>
            </div></RouterLink
          >
        </div>
        <div v-if="!browseHistory.length" class="empty">
          <h2>还没有浏览记录</h2>
          <RouterLink class="action" to="/search">去发现好住处</RouterLink>
        </div>
      </template>
      <template v-else-if="pageType === 'compare'"
        ><header class="page-heading">
          <span>选得更清楚</span>
          <h1>房源对比</h1>
          <p>最多对比3家房源；参考起价，实际库存和价格以所选日期为准。</p>
        </header>
        <div v-if="compareHotels.length" class="compare-grid">
          <article v-for="item in compareHotels" :key="item.id">
            <img :src="item.image" :alt="item.name" />
            <h2>{{ item.name }}</h2>
            <dl>
              <dt>位置</dt>
              <dd>{{ item.city }} · {{ item.district }}</dd>
              <dt>类型</dt>
              <dd>{{ item.type || '未提供' }}</dd>
              <dt>住客评分</dt>
              <dd>{{ item.score || '暂无' }}</dd>
              <dt>参考起价</dt>
              <dd class="price">¥{{ item.price }} / 晚起</dd>
            </dl>
            <RouterLink class="action" :to="'/hotel/' + item.id">查看房型</RouterLink
            ><button class="text-button" @click="toggleCompare(item)">移出对比</button>
          </article>
        </div>
        <div v-else class="empty">
          <h2>还没有选择对比房源</h2>
          <RouterLink class="action" to="/search">去房源列表添加</RouterLink>
        </div></template
      >
      <template v-else-if="pageType === 'help'"
        ><header class="page-heading">
          <span>住前住后，都有指引</span>
          <h1>预订帮助</h1>
          <p>查找常见问题，或进入订单与商家沟通处理。</p>
        </header>
        <div class="help-actions">
          <RouterLink to="/user/orders">查看我的订单 →</RouterLink
          ><RouterLink to="/user/favorites">查看我的收藏 →</RouterLink>
        </div>
        <details v-for="[question, answer] in faqs" :key="question" class="faq">
          <summary>{{ question }}</summary>
          <p>{{ answer }}</p>
        </details></template
      >
      <div v-else class="empty">
        <h1>内容暂未找到</h1>
        <RouterLink to="/">返回首页</RouterLink>
      </div>
    </main>
    <Footer />
  </div>
</template>
<style scoped>
.destination-placeholder {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 15px;
  background: #e9edf4;
  color: #627396;
  padding: 24px;
  box-sizing: border-box;
}
.destination-placeholder strong {
  font-size: 25px;
  color: #253e5e;
}
.explore-filter {
  flex-wrap: wrap;
}
.explore-page {
  background: #f6f9fc;
  min-height: 100vh;
  color: #19364f;
  font-family: var(--font-ui);
}
.explore-main {
  width: min(100% - 48px, 1220px);
  margin: auto;
  padding: 24px 0 70px;
}
.breadcrumbs {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #8a9aab;
  margin-bottom: 30px;
}
.breadcrumbs a {
  color: #637f99;
  text-decoration: none;
}
.page-heading {
  margin: 20px 0 30px;
}
.page-heading > span,
.detail small {
  color: #2585cc;
  font-weight: 700;
  font-size: 12px;
}
.page-heading h1 {
  font-size: 34px;
  margin: 12px 0;
}
.page-heading p {
  color: #7b8ea1;
  line-height: 1.8;
  font-size: 14px;
}
.explore-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 26px;
}
.explore-filter input,
.explore-filter select {
  min-width: 0;
  padding: 12px;
  border: 1px solid #dce5ee;
  background: white;
  border-radius: 6px;
}
.explore-filter input {
  width: 320px;
}
.explore-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}
.explore-card {
  min-width: 0;
  color: inherit;
  text-decoration: none;
  background: white;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e1e9f1;
}
.explore-card img {
  width: 100%;
  aspect-ratio: 1.7;
  object-fit: cover;
  background: #e7eff4;
}
.explore-card > div {
  padding: 20px;
}
.explore-card small {
  color: #6086a5;
  font-size: 12px;
}
.explore-card h2 {
  font-size: 18px;
  line-height: 1.6;
  margin: 10px 0;
}
.explore-card p {
  color: #8a99a7;
  font-size: 13px;
  line-height: 1.8;
}
.explore-card span {
  font-size: 12px;
  color: #2683cc;
}
.detail {
  background: white;
  border: 1px solid #e2eaf1;
  border-radius: 16px;
  overflow: hidden;
  max-width: 950px;
  margin: auto;
}
.detail-cover {
  width: 100%;
  height: 360px;
  object-fit: cover;
}
.detail-body {
  padding: 35px;
}
.detail h1 {
  font-size: 32px;
  line-height: 1.5;
}
.detail h2 {
  font-size: 21px;
  margin-top: 30px;
}
.detail p,
.itinerary li {
  line-height: 2;
  color: #627b90;
}
.itinerary li {
  padding: 7px;
}
.notice {
  padding: 18px;
  background: #f4f8fc;
  border-left: 3px solid #4097ee;
}
.action {
  display: inline-block;
  background: #4097ee;
  color: white;
  text-decoration: none;
  padding: 12px 20px;
  border-radius: 7px;
  font-size: 14px;
}
.detail .credit {
  font-size: 10px;
  margin-top: 30px;
}
.credit a,
.text-link {
  color: #718ba0;
}
.text-link {
  display: block;
  margin-top: 20px;
  font-size: 12px;
}
.empty {
  text-align: center;
  padding: 60px 15px;
  color: #7b8ea1;
}
.text-button {
  border: 0;
  background: none;
  color: #3c84ba;
  cursor: pointer;
  padding: 12px;
}
.compare-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}
.compare-grid article {
  padding: 20px;
  background: white;
  border: 1px solid #e2eaf1;
  border-radius: 12px;
  min-width: 0;
}
.compare-grid img {
  width: 100%;
  aspect-ratio: 1.6;
  object-fit: cover;
  border-radius: 8px;
}
.compare-grid h2 {
  font-size: 18px;
}
.compare-grid dt {
  font-size: 12px;
  color: #8a9cae;
}
.compare-grid dd {
  margin: 8px 0 24px;
  font-size: 14px;
}
.price {
  color: #ff6555;
}
.help-actions {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
}
.help-actions a {
  color: #2582ca;
  font-size: 14px;
}
.faq {
  padding: 22px;
  margin: 12px 0;
  border: 1px solid #e3eaf1;
  border-radius: 9px;
  background: white;
}
.faq summary {
  cursor: pointer;
  font-weight: 600;
}
.faq p {
  line-height: 1.9;
  color: #6a8197;
  font-size: 14px;
}
@media (max-width: 760px) {
  .explore-main {
    width: calc(100% - 32px);
  }
  .explore-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }
  .page-heading h1 {
    font-size: 27px;
  }
  .explore-card > div {
    padding: 12px;
  }
  .explore-card h2 {
    font-size: 15px;
  }
  .detail-body {
    padding: 22px;
  }
  .detail-cover {
    height: 230px;
  }
  .detail h1 {
    font-size: 26px;
  }
  .compare-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 420px) {
  .explore-grid {
    grid-template-columns: 1fr;
  }
}
</style>
