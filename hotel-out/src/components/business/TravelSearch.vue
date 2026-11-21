<script setup>
import dayjs from 'dayjs'
import { reactive, ref, watch } from 'vue'
import { useAppStore } from '@/stores'
import { useRouter } from 'vue-router'
import { CITIES } from '@/constants/locations'
import { getStayNights } from '@/utils/date'
const router = useRouter()
const appStore = useAppStore()
const form = reactive({
  city: appStore.selectedCity,
  keyword: '',
  checkIn: dayjs().format('YYYY-MM-DD'),
  checkOut: dayjs().add(1, 'day').format('YYYY-MM-DD'),
  guests: 2,
})
watch(
  () => appStore.selectedCity,
  (city) => {
    form.city = city
  },
)
const error = ref('')
function submit() {
  if (form.checkIn < dayjs().format('YYYY-MM-DD') || !getStayNights(form.checkIn, form.checkOut)) {
    error.value = '请选择有效日期，离店日期必须晚于入住日期'
    return
  }
  error.value = ''
  router.push({ name: 'client-search', query: { ...form } })
}
</script>
<template>
  <section class="travel-search">
    <div class="search-intro">
      <span>住得舒心 · 玩得尽兴</span>
      <h1>下一站，住进喜欢的生活</h1>
      <p>找酒店、选民宿，也发现旅途中的好风景</p>
    </div>
    <form class="search-form" @submit.prevent="submit">
      <label
        >目的地<select
          v-model="form.city"
          aria-label="目的地城市"
          @change="appStore.setSelectedCity(form.city)"
        >
          <option value="">全部城市</option>
          <option v-for="city in CITIES" :key="city">{{ city }}</option>
        </select></label
      >
      <label
        >入住日期<input
          v-model="form.checkIn"
          aria-label="入住日期"
          type="date"
          :min="dayjs().format('YYYY-MM-DD')"
          required
      /></label>
      <label
        >离店日期<input
          v-model="form.checkOut"
          aria-label="离店日期"
          type="date"
          :min="form.checkIn"
          required
      /></label>
      <label
        >入住人数<select v-model.number="form.guests" aria-label="入住人数">
          <option v-for="n in 6" :key="n" :value="n">{{ n }} 位住客</option>
        </select></label
      >
      <label class="keyword"
        >酒店 / 关键词<input v-model.trim="form.keyword" placeholder="酒店名、商圈或地标"
      /></label>
      <button type="submit">搜索住宿</button>
    </form>
    <p v-if="error" role="alert" class="error">{{ error }}</p>
    <div class="hot-cities">
      <span>热门目的地</span
      ><RouterLink
        v-for="city in ['北京', '上海', '广州', '深圳', '杭州', '成都']"
        :key="city"
        :to="{ name: 'client-search', query: { ...form, city } }"
        >{{ city }}</RouterLink
      >
    </div>
  </section>
</template>
<style scoped>
.travel-search {
  margin-bottom: 40px;
  padding: 30px;
  border: 1px solid #dbe8f7;
  border-radius: 18px;
  background: #f2f8ff;
  color: #173754;
}
.search-intro span {
  color: #377db6;
  font-size: 12px;
  font-weight: 700;
}
.search-intro h1 {
  font:
    750 32px/1.4 'Microsoft YaHei',
    sans-serif;
  margin: 8px 0;
}
.search-intro p {
  color: #6f8498;
  font-size: 13px;
  margin: 0 0 25px;
}
.search-form {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 0.8fr 1.3fr auto;
  gap: 10px;
}
.search-form label {
  display: grid;
  gap: 9px;
  min-width: 0;
  padding: 12px;
  background: white;
  border: 1px solid #dfe8f1;
  border-radius: 8px;
  color: #788b9d;
  font-size: 11px;
}
.search-form input,
.search-form select {
  width: 100%;
  min-width: 0;
  border: 0;
  background: white;
  color: #203b53;
  font: inherit;
  font-size: 14px;
  min-height: 26px;
}
.search-form button {
  padding: 0 24px;
  border: 0;
  border-radius: 8px;
  background: #ff7546;
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}
.hot-cities {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 20px;
  font-size: 12px;
}
.hot-cities span {
  color: #8295a5;
}
.hot-cities a {
  color: #496b89;
  text-decoration: none;
}
.error {
  color: #b83e25;
  font-size: 12px;
}
@media (max-width: 1050px) {
  .search-form {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .search-form button {
    min-height: 56px;
  }
}
@media (max-width: 600px) {
  .travel-search {
    padding: 20px 15px;
  }
  .search-intro h1 {
    font-size: 24px;
  }
  .search-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .keyword {
    grid-column: 1/-1;
  }
  .search-form button {
    grid-column: 1/-1;
  }
  .hot-cities {
    gap: 12px;
  }
}
</style>
