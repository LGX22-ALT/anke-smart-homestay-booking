<script setup>
import { Close, Expand, Fold, Menu, SwitchButton } from '@element-plus/icons-vue'
import { computed, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'

import { logoutSession } from '@/services/dataSource'
import { useAppStore, useUserStore } from '@/stores'
import UserAvatar from '@/components/common/UserAvatar.vue'

const props = defineProps({
  title: { type: String, default: 'HOTEL OUT' },
  roleLabel: { type: String, default: '管理后台' },
  accent: { type: String, default: '#137c8b' },
  navigation: { type: Array, default: () => [] },
})
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const mobileOpen = ref(false)
const mobileViewport = ref(false)
const menuToggle = ref(null)
const drawerClose = ref(null)
let media
let originalOverflow = ''
const drawerOpen = computed(() => mobileOpen.value && mobileViewport.value)
function trapDrawerFocus(event) {
  if (!drawerOpen.value) return
  const items = [...event.currentTarget.querySelectorAll('a[href],button:not([disabled])')].filter(
    (e) => e.getClientRects().length,
  )
  const first = items[0],
    last = items[items.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first?.focus()
  }
}
function resizeDrawer() {
  mobileViewport.value = media.matches
  if (!media.matches) mobileOpen.value = false
}
watch(drawerOpen, async (open) => {
  if (open) {
    originalOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  } else document.body.style.overflow = originalOverflow
  await nextTick()
  if (open) drawerClose.value?.focus()
  else if (mobileViewport.value) menuToggle.value?.focus()
})
onMounted(() => {
  media = window.matchMedia('(max-width: 800px)')
  resizeDrawer()
  media.addEventListener('change', resizeDrawer)
})
onBeforeUnmount(() => {
  media?.removeEventListener('change', resizeDrawer)
  if (drawerOpen.value) document.body.style.overflow = originalOverflow
})
const sidebarClass = computed(() => ({
  collapsed: appStore.sidebarCollapsed,
  open: mobileOpen.value,
}))

async function signOut() {
  await logoutSession()
  userStore.clearSession()
  router.replace('/login')
}
</script>

<template>
  <div
    class="office-shell"
    :style="{ '--office-accent': props.accent }"
    @keydown.esc="mobileOpen = false"
  >
    <button
      v-if="drawerOpen"
      class="office-backdrop"
      type="button"
      aria-label="关闭导航遮罩"
      tabindex="-1"
      @click="mobileOpen = false"
    />
    <aside
      id="office-drawer"
      :class="['office-sidebar', sidebarClass]"
      :inert="mobileViewport && !mobileOpen ? true : undefined"
      @keydown.tab="trapDrawerFocus"
    >
      <button
        ref="drawerClose"
        class="drawer-close"
        type="button"
        aria-label="收起手机导航"
        @click="mobileOpen = false"
      >
        <Close />
      </button>
      <RouterLink class="office-brand" to="/"
        ><span>H</span>
        <div>
          <strong>{{ title }}</strong
          ><small>{{ roleLabel }}</small>
        </div></RouterLink
      >
      <nav>
        <RouterLink
          v-for="item in navigation"
          :key="item.to"
          :to="item.to"
          @click="mobileOpen = false"
        >
          <span class="nav-icon"><component :is="item.icon" /></span
          ><span class="nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>
      <button
        class="sidebar-collapse"
        type="button"
        :aria-label="appStore.sidebarCollapsed ? '展开侧栏' : '收起侧栏'"
        @click="appStore.toggleSidebar()"
      >
        <component :is="appStore.sidebarCollapsed ? Expand : Fold" /><span>{{
          appStore.sidebarCollapsed ? '展开' : '收起侧栏'
        }}</span>
      </button>
    </aside>
    <div class="office-main" :inert="drawerOpen ? true : undefined">
      <header class="office-topbar">
        <button
          ref="menuToggle"
          class="mobile-menu"
          type="button"
          aria-label="打开导航"
          :aria-expanded="drawerOpen"
          aria-controls="office-drawer"
          @click="mobileOpen = !mobileOpen"
        >
          <Menu />
        </button>
        <div>
          <strong>{{ roleLabel }}</strong
          ><span>住见旅宿 · 运营状态在线</span>
        </div>
        <div class="operator">
          <RouterLink
            v-if="userStore.role === 'merchant'"
            class="operator-profile"
            to="/merchant/profile"
            aria-label="编辑个人资料"
          >
            <UserAvatar
              :src="userStore.userInfo?.avatar || ''"
              :name="userStore.userInfo?.name || '商家'"
            />
            <span>{{ userStore.userInfo?.name || '商家' }}</span>
          </RouterLink>
          <span v-else>{{ userStore.userInfo?.name || '运营人员' }}</span>
          <button type="button" aria-label="退出登录" title="退出登录" @click="signOut">
            <SwitchButton />
          </button>
        </div>
      </header>
      <main class="office-content"><RouterView /></main>
    </div>
  </div>
</template>

<style scoped>
.office-backdrop {
  position: fixed;
  inset: 0;
  z-index: 19;
  border: 0;
  background: rgb(20 24 36 / 45%);
}
.drawer-close {
  display: none;
}
@media (max-width: 800px) {
  .drawer-close {
    display: grid;
    position: absolute;
    top: 16px;
    right: 8px;
    width: 44px;
    height: 44px;
    padding: 12px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: white;
    cursor: pointer;
  }
  .office-brand {
    padding-right: 44px;
  }
}
.office-shell {
  display: grid;
  grid-template-columns: auto 1fr;
  min-height: 100vh;
  color: #243b53;
  background: #f0f4f8;
}
.office-sidebar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  flex-direction: column;
  width: 230px;
  height: 100vh;
  color: var(--line);
  background: var(--ink);
  transition: width 0.2s;
}
.office-sidebar.collapsed {
  width: 76px;
}
.office-brand {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 70px;
  padding: 0 20px;
  border-bottom: 1px solid rgb(255 255 255 / 10%);
  color: #fff;
  text-decoration: none;
}
.office-brand > span {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border: 1px solid var(--office-accent);
  border-radius: 50%;
  color: var(--office-accent);
  font-family: var(--font-ui);
}
.office-brand div {
  display: grid;
  gap: 3px;
  overflow: hidden;
  white-space: nowrap;
}
.office-brand small {
  color: var(--muted);
  font-size: 9px;
  letter-spacing: 0.12em;
}
.office-sidebar nav {
  display: grid;
  gap: 3px;
  padding: 18px 10px;
}
.office-sidebar nav a {
  display: flex;
  gap: 12px;
  align-items: center;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 4px;
  color: #bcccdc;
  text-decoration: none;
}
.office-sidebar nav a.router-link-active {
  color: #fff;
  background: color-mix(in srgb, var(--office-accent) 25%, transparent);
}
.nav-icon {
  display: grid;
  width: 28px;
  flex: 0 0 28px;
  place-items: center;
  color: var(--office-accent);
}
.nav-icon :deep(svg) {
  width: 17px;
  height: 17px;
}
.office-sidebar.collapsed .nav-label,
.office-sidebar.collapsed .office-brand div,
.office-sidebar.collapsed .sidebar-collapse span {
  display: none;
}
.sidebar-collapse {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin: auto 12px 16px;
  padding: 10px;
  border: 1px solid rgb(255 255 255 / 14%);
  color: #9fb3c8;
  background: transparent;
  cursor: pointer;
}
.sidebar-collapse svg {
  width: 15px;
}
.office-main {
  min-width: 0;
}
.office-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 70px;
  padding: 0 26px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.office-topbar > div:first-of-type {
  display: grid;
  gap: 3px;
}
.office-topbar span {
  color: var(--muted);
  font-size: 11px;
}
.operator {
  display: flex;
  gap: 12px;
  align-items: center;
}
.operator button,
.mobile-menu {
  display: grid;
  width: 34px;
  height: 34px;
  padding: 7px;
  border: 0;
  place-items: center;
  color: #52606d;
  background: transparent;
  cursor: pointer;
}
.operator-profile {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}
.operator-profile > span:last-child {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 600px) {
  .operator-profile > span:last-child {
    max-width: 80px;
  }
}
.mobile-menu {
  display: none;
}
.office-content {
  padding: 26px;
}
.office-sidebar.collapsed .office-brand {
  padding: 0 20px;
}
@media (max-width: 800px) {
  .office-shell {
    grid-template-columns: 1fr;
  }
  .office-sidebar {
    position: fixed;
    left: -250px;
    width: 230px;
    box-shadow: 10px 0 40px rgb(16 42 67 / 24%);
  }
  .office-sidebar.open {
    left: 0;
  }
  .office-sidebar.collapsed {
    width: 230px;
  }
  .office-sidebar.collapsed .nav-label,
  .office-sidebar.collapsed .office-brand div,
  .office-sidebar.collapsed .sidebar-collapse span {
    display: initial;
  }
  .mobile-menu {
    display: grid;
  }
  .office-topbar {
    padding: 0 16px;
  }
  .office-content {
    padding: 18px;
  }
}
</style>
