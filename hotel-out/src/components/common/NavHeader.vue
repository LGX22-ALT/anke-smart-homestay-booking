<script setup>
import {
  ArrowDown,
  House,
  HomeFilled,
  LocationInformation,
  Tickets,
  Reading,
  UserFilled,
  Service,
  Grid,
  ChatDotRound,
} from '@element-plus/icons-vue'
import { computed, ref } from 'vue'
import { ElPopover } from 'element-plus'
import 'element-plus/es/components/popover/style/css'
import { useRoute, useRouter } from 'vue-router'
import { CITIES } from '@/constants/locations'
import { logoutSession } from '@/services/dataSource'
import { useAppStore, useUserStore } from '@/stores'
import { compareHotels } from '@/utils/travelStorage'
import UserAvatar from '@/components/common/UserAvatar.vue'
defineProps({ dark: { type: Boolean, default: false } })
const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()
const cityVisible = ref(false)
const cityKeyword = ref('')
const cityOptions = computed(() => CITIES.filter((city) => city.includes(cityKeyword.value.trim())))
const displayName = computed(
  () => userStore.userInfo?.name || userStore.userInfo?.username || '住客',
)
const mobileNavigation = computed(() => [
  { label: 'AI', to: { name: 'ai-chat' }, icon: ChatDotRound, active: route.name === 'ai-chat' },
  { label: '首页', to: '/', icon: HomeFilled, active: route.path === '/' },
  {
    label: '酒店',
    to: { name: 'client-search', query: { city: appStore.selectedCity || undefined } },
    icon: House,
    active: ['client-search', 'hotel-detail', 'order-confirm', 'pay-result'].includes(route.name),
  },
  {
    label: '景点',
    to: '/attractions',
    icon: LocationInformation,
    active: ['client-attractions', 'attraction-detail', 'client-guides', 'guide-detail'].includes(
      route.name,
    ),
  },
  {
    label: '我的',
    to: userStore.role === 'merchant' ? '/merchant/profile' : '/user/profile',
    icon: UserFilled,
    active: route.path.startsWith('/user/') || route.name === 'client-messages',
  },
])
const navigation = computed(() => [
  {
    label: 'AI 旅行顾问',
    to: { name: 'ai-chat' },
    icon: ChatDotRound,
    active: route.name === 'ai-chat',
  },
  {
    label: '首页',
    to: { name: 'client-home' },
    icon: HomeFilled,
    active: route.name === 'client-home',
  },
  {
    label: '酒店民宿',
    to: { name: 'client-search', query: { city: appStore.selectedCity || undefined } },
    icon: House,
    active: ['client-search', 'hotel-detail'].includes(route.name),
  },
  {
    label: '景点',
    to: { name: 'client-attractions' },
    icon: LocationInformation,
    active: ['client-attractions', 'attraction-detail'].includes(route.name),
  },
  {
    label: '攻略',
    to: { name: 'client-guides' },
    icon: Reading,
    active: ['client-guides', 'guide-detail'].includes(route.name),
  },
])
const moreItems = [
  {
    label: '我的私信',
    to: userStore.role === 'merchant' ? '/merchant/messages' : '/messages',
    icon: Tickets,
  },
  { label: '民宿订单', to: '/user/orders', icon: Tickets },
  { label: '我的收藏', to: '/user/favorites', icon: House },
  { label: '最近浏览', to: '/history', icon: Reading },
  { label: '房源对比', to: '/compare', icon: Grid },
  {
    label: '个人中心',
    to: userStore.role === 'merchant' ? '/merchant/profile' : '/user/profile',
    icon: UserFilled,
  },
  { label: '账户充值', to: '/user/recharge', icon: Tickets },
  { label: '商家中心', to: '/merchant/dashboard', icon: House },
]
function chooseCity(city) {
  appStore.setSelectedCity(city)
  cityVisible.value = false
  cityKeyword.value = ''
  if (route.name === 'client-search') {
    router.replace({
      name: 'client-search',
      query: { ...route.query, city: city || undefined, district: undefined, pageNum: 1 },
    })
  }
}
async function signOut() {
  await logoutSession()
  userStore.clearSession()
  await router.push({ name: 'client-home' })
}
</script>
<template>
  <header class="site-header">
    <div class="header-inner">
      <div class="brand-area">
        <RouterLink class="brand" to="/" aria-label="民宿预约系统首页"
          ><span class="brand-mark" aria-hidden="true"><HomeFilled /></span
          ><span>民宿预约系统<span class="brand-caption">发现好住处</span></span></RouterLink
        >
        <el-popover
          v-model:visible="cityVisible"
          trigger="click"
          :width="320"
          placement="bottom-start"
          :teleported="true"
        >
          <template #reference
            ><button
              class="city-switch"
              type="button"
              aria-label="切换城市"
              :aria-expanded="cityVisible"
            >
              <span>{{ appStore.selectedCity || '全国' }}</span
              ><ArrowDown /></button
          ></template>
          <section class="city-picker" aria-label="选择所在城市" @keydown.esc="cityVisible = false">
            <div class="picker-title">
              <strong>切换城市</strong><span>当前：{{ appStore.selectedCity || '全国' }}</span>
            </div>
            <input
              v-model="cityKeyword"
              class="city-search"
              aria-label="搜索城市"
              placeholder="输入城市名称"
            />
            <div class="city-options">
              <button
                v-if="!cityKeyword"
                type="button"
                :class="{ selected: !appStore.selectedCity }"
                @click="chooseCity('')"
              >
                全国
              </button>
              <button
                v-for="city in cityOptions"
                :key="city"
                type="button"
                :class="{ selected: appStore.selectedCity === city }"
                @click="chooseCity(city)"
              >
                {{ city }}
              </button>
            </div>
            <p v-if="!cityOptions.length" class="picker-note">暂无匹配城市，请尝试其他城市名称。</p>
            <p class="picker-note">手动选择浏览城市，用于推荐和住宿搜索。</p>
          </section>
        </el-popover>
      </div>
      <nav class="site-nav" aria-label="主导航">
        <RouterLink
          v-for="item in navigation"
          :key="item.label"
          class="site-nav-item"
          :class="{ 'site-nav-item--active': item.active }"
          :to="item.to"
          ><component :is="item.icon" aria-hidden="true" /><span>{{ item.label }}</span></RouterLink
        >
      </nav>
      <div class="header-actions">
        <RouterLink to="/help" class="help-entry" aria-label="预订帮助" title="预订帮助"
          ><Service
        /></RouterLink>
        <span class="action-divider" aria-hidden="true"></span>
        <el-popover
          trigger="click"
          placement="bottom-end"
          :width="220"
          :show-after="80"
          popper-class="user-menu-popper"
        >
          <template #reference>
            <button
              class="user-entry user-menu-trigger"
              type="button"
              :title="displayName"
              :aria-label="userStore.isAuthenticated ? '打开账户菜单' : '登录或注册'"
              @click="!userStore.isAuthenticated && router.push({ name: 'client-login' })"
            >
              <UserAvatar
                v-if="userStore.isAuthenticated"
                :src="userStore.userInfo?.avatar || ''"
                :name="displayName"
              />
              <span v-else class="user-avatar"><UserFilled /></span>
              <span class="user-name">{{
                userStore.isAuthenticated ? 'Hi，' + displayName : '登录 / 注册'
              }}</span>
              <ArrowDown class="user-menu-arrow" />
            </button>
          </template>
          <nav class="more-menu user-menu" aria-label="账户功能">
            <button
              v-if="userStore.isAuthenticated"
              class="mobile-menu-signout"
              type="button"
              @click="signOut"
            >
              退出登录
            </button>
            <RouterLink v-for="item in moreItems" :key="item.label" :to="item.to"
              ><component :is="item.icon" /><span>{{ item.label }}</span
              ><b v-if="item.to === '/compare' && compareHotels.length">{{
                compareHotels.length
              }}</b></RouterLink
            >
          </nav>
        </el-popover>
        <template v-if="userStore.isAuthenticated">
          <button type="button" class="signout-button" @click="signOut">退出</button>
        </template>
      </div>
    </div>
  </header>
  <nav class="mobile-tabbar" aria-label="手机导航">
    <RouterLink
      v-for="item in mobileNavigation"
      :key="item.label"
      :to="item.to"
      :aria-current="item.active ? 'page' : undefined"
    >
      <component :is="item.icon" aria-hidden="true" /><span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>
<style scoped>
.site-header {
  position: relative;
  z-index: 20;
  width: 100%;
  background: #fff;
  border-bottom: 1px solid #e8eef0;
  color: #192b38;
  font-family: var(--font-ui);
}
.header-inner {
  width: min(100% - 64px, 1440px);
  min-height: 88px;
  margin: auto;
  display: flex;
  align-items: center;
  gap: 32px;
}
.brand-area {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #59359b;
  text-decoration: none;
  font-size: 25px;
  line-height: 1.1;
  font-weight: 800;
  white-space: nowrap;
  letter-spacing: -0.04em;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 40px;
  color: #fff;
  background: #6238a5;
  border-radius: 15px 15px 15px 5px;
  transform: rotate(-6deg);
  box-shadow: 5px 4px 0 #f4ce4d;
}
.brand-mark svg {
  width: 26px;
  height: 26px;
  transform: rotate(6deg);
}
.brand-caption {
  display: block;
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.2em;
  margin-top: 5px;
  color: #9581b3;
}
.city-switch {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 0;
  border: 0;
  background: none;
  color: #5d6875;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
.city-switch svg {
  width: 11px;
  height: 11px;
}
.site-nav {
  display: flex;
  align-items: stretch;
  align-self: stretch;
  gap: 22px;
  margin: 0 auto;
  min-width: 0;
}
.site-nav-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #142331;
  text-decoration: none;
  font-size: 17px;
  font-weight: 700;
  white-space: nowrap;
}
.site-nav-item svg {
  width: 27px;
  height: 27px;
  color: #00c777;
}
.site-nav-item--active:after {
  content: '';
  position: absolute;
  bottom: 16px;
  left: 50%;
  width: 23px;
  height: 7px;
  border-bottom: 3px solid #00c777;
  border-radius: 0 0 50% 50%;
  transform: translateX(-50%);
}
.site-nav-item:hover {
  color: #00ad6b;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
  white-space: nowrap;
  font-size: 12px;
}
.help-entry {
  display: flex;
  color: #6535a6;
}
.help-entry svg {
  width: 23px;
  height: 23px;
}
.action-divider {
  height: 20px;
  width: 1px;
  background: #e4e8ee;
}
.user-entry {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #192b38;
  text-decoration: none;
  font-weight: 600;
}
.user-menu-trigger {
  padding: 8px 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}
.user-menu-arrow {
  width: 11px;
  height: 11px;
  color: #6e7d89;
}
.user-avatar {
  display: none;
  align-items: center;
  justify-content: center;
  width: 25px;
  height: 25px;
  border-radius: 50%;
  color: #6535a6;
  background: #f1ebf8;
}
.user-avatar svg {
  width: 16px;
  height: 16px;
}
.user-name {
  max-width: 115px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.signout-button {
  padding: 8px 0;
  border: 0;
  background: none;
  color: #192b38;
  cursor: pointer;
  font: inherit;
}
.user-menu-trigger:focus-visible {
  outline: 2px solid #633ba1;
  outline-offset: 5px;
  border-radius: 3px;
}
.city-switch:focus-visible,
.site-nav a:focus-visible,
.header-actions a:focus-visible,
.signout-button:focus-visible {
  outline: 2px solid #633ba1;
  outline-offset: 5px;
  border-radius: 3px;
}
.city-picker {
  font-family: var(--font-ui);
}
.picker-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0 16px;
  color: #293c4d;
}
.picker-title strong {
  font-size: 15px;
}
.picker-title span {
  font-size: 11px;
  color: #8091a1;
}
.city-search {
  width: 100%;
  padding: 10px;
  border: 1px solid #dce5eb;
  border-radius: 6px;
  font-size: 13px;
}
.city-options {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  margin-top: 16px;
}
.city-options button {
  padding: 9px 0;
  border: 0;
  background: #f5f7f9;
  border-radius: 5px;
  color: #4a6276;
  font-size: 12px;
  cursor: pointer;
}
.city-options button:hover,
.city-options button.selected {
  background: #e7f9ef;
  color: #009a5d;
}
.picker-note {
  font-size: 11px;
  color: #8b99a6;
  line-height: 1.8;
  margin: 15px 0 3px;
}
.more-menu {
  display: grid;
  gap: 3px;
}
.more-menu a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px;
  color: #354c61;
  text-decoration: none;
  font-size: 13px;
  border-radius: 5px;
}
.more-menu a:hover {
  background: #eefaf4;
  color: #009a5d;
}
.more-menu svg {
  width: 17px;
  height: 17px;
  color: #00b66e;
}
.more-menu b {
  margin-left: auto;
  color: #6535a6;
  font-size: 11px;
}
:deep(.user-menu-popper) {
  padding: 9px !important;
  border: 1px solid #e6eaf0 !important;
  border-radius: 10px !important;
  box-shadow: 0 14px 34px rgb(20 43 64 / 16%) !important;
}
:deep(.user-menu-popper .more-menu) {
  display: grid;
  gap: 3px;
}
:deep(.user-menu-popper .more-menu a) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px;
  color: #354c61;
  text-decoration: none;
  font-size: 13px;
  border-radius: 5px;
}
:deep(.user-menu-popper .more-menu a:hover) {
  background: #eefaf4;
  color: #009a5d;
}
:deep(.user-menu-popper .more-menu svg) {
  width: 17px;
  height: 17px;
  color: #00b66e;
}
:deep(.user-menu-popper .more-menu b) {
  margin-left: auto;
  color: #6535a6;
  font-size: 11px;
}
@media (max-width: 1200px) {
  .header-inner {
    width: calc(100% - 40px);
    gap: 20px;
  }
  .site-nav {
    gap: 16px;
  }
  .site-nav-item {
    font-size: 15px;
    gap: 6px;
  }
  .site-nav-item svg {
    width: 23px;
  }
  .brand {
    font-size: 22px;
  }
  .brand-area {
    gap: 16px;
  }
  .header-actions {
    gap: 13px;
  }
  .user-name {
    max-width: 85px;
  }
}
@media (max-width: 980px) {
  .header-inner {
    flex-wrap: wrap;
    gap: 0;
    min-height: auto;
    padding-top: 15px;
  }
  .brand-area {
    margin-right: auto;
  }
  .header-actions {
    min-height: 45px;
  }
  .site-nav {
    order: 3;
    width: 100%;
    gap: 28px;
    justify-content: center;
    min-height: 64px;
  }
  .site-nav-item--active:after {
    bottom: 5px;
  }
}
@media (max-width: 520px) {
  .header-inner {
    width: calc(100% - 28px);
    padding-top: 12px;
  }
  .brand {
    font-size: 19px;
    gap: 8px;
  }
  .brand-mark {
    width: 32px;
    height: 31px;
    border-radius: 10px 10px 10px 3px;
  }
  .brand-mark svg {
    width: 20px;
  }
  .brand-caption {
    font-size: 8px;
  }
  .brand-area {
    gap: 14px;
  }
  .city-switch {
    font-size: 12px;
  }
  .header-actions {
    gap: 8px;
    font-size: 11px;
  }
  .help-entry,
  .action-divider {
    display: none;
  }
  .user-avatar {
    display: flex;
  }
  .user-name {
    max-width: 60px;
  }
  .site-nav {
    min-height: 60px;
    gap: 0;
    justify-content: space-between;
  }
  .site-nav-item {
    gap: 5px;
    font-size: 13px;
  }
  .site-nav-item svg {
    width: 20px;
    height: 20px;
  }
  .site-nav-item--active:after {
    bottom: 6px;
  }
}
@media (prefers-reduced-motion: reduce) {
  * {
    scroll-behavior: auto;
  }
}
</style>
