<script setup>
import { ElMessage } from 'element-plus'
import { House } from '@element-plus/icons-vue'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { login as apiLogin, register as apiRegister, useMock } from '@/services/dataSource'
import { useUserStore } from '@/stores'
import { USER_ROLES } from '@/utils/auth'
import { isValidEmail, isValidMobile } from '@/utils/validate'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mode = ref('login')
const entrance = ref(USER_ROLES.USER)
const showPassword = ref(false)
const entranceTitle = computed(
  () => ({ user: '欢迎回来', merchant: '商家登录', admin: '管理员登录' })[entrance.value],
)

function switchEntrance(role) {
  if (loading.value) return
  entrance.value = role
  switchMode('login')
}

function switchMode(value) {
  if (loading.value) return
  formError.value = ''
  mode.value = value
  form.password = ''
  showPassword.value = false
}
const form = reactive({
  name: '',
  phone: '',
  email: '',
  password: '',
})
const loading = ref(false)
const formError = ref('')

function showWarning(message) {
  formError.value = message
}

function validateForm() {
  if (mode.value === 'register') {
    const username = form.name.trim()
    const email = form.email.trim()

    if (username.length < 2 || username.length > 64) {
      showWarning('用户名长度应为 2-64 个字符')
      return false
    }

    if (!isValidEmail(email) || email.length > 128) {
      showWarning('请输入正确的邮箱地址')
      return false
    }
  }

  if (!isValidMobile(form.phone)) {
    showWarning('请输入正确的手机号码')
    return false
  }

  if (form.password.length < 6 || form.password.length > 32) {
    showWarning('密码长度应为 6-32 个字符')
    return false
  }

  return true
}

const redirectTarget = computed(() => {
  const target = String(route.query.redirect || '')
  return target.startsWith('/') && !target.startsWith('//') && target.split(/[?#]/)[0] !== '/login'
    ? target
    : ''
})

function destinationFor(role) {
  return (
    redirectTarget.value ||
    { user: '/', merchant: '/merchant/dashboard', admin: '/admin/dashboard' }[role] ||
    '/'
  )
}

const accounts = [
  {
    role: USER_ROLES.USER,
    label: '住客账号',
    name: '林晓',
    phone: '13800000003',
    destination: '/',
  },
  {
    role: USER_ROLES.MERCHANT,
    label: '商家账号',
    name: '山里见民宿',
    phone: '13800000002',
    destination: '/merchant/dashboard',
  },
  {
    role: USER_ROLES.ADMIN,
    label: '管理员账号',
    name: '平台运营员',
    phone: '13800000001',
    destination: '/admin/dashboard',
  },
]

function loginAs(account) {
  if (!useMock) return
  userStore.clearSession()
  userStore.setSession({
    token: `mock-${account.role}-${Date.now()}`,
    userInfo: { id: Date.now(), name: account.name, phone: account.phone },
    role: account.role,
  })
  ElMessage.success(`已登录为${account.label}`)
  router.replace(destinationFor(account.role))
}

async function submit() {
  if (loading.value) return
  formError.value = ''
  if (!validateForm()) return

  if (!useMock) {
    loading.value = true
    try {
      const session =
        mode.value === 'register'
          ? await apiRegister({
              phone: form.phone,
              email: form.email,
              username: form.name,
              password: form.password,
              name: form.name || '住客',
            })
          : await apiLogin({ phone: form.phone, password: form.password })
      userStore.setSession(session)
      await router.replace(destinationFor(session.role))
    } catch {
      // The request layer presents the server error; keep the form usable.
    } finally {
      loading.value = false
    }
    return
  }
  const account = accounts.find((item) => item.role === entrance.value)
  loginAs({ ...account, phone: form.phone, name: form.name || account.name })
}
</script>

<template>
  <main class="login-page">
    <RouterLink class="brand" to="/"
      ><span aria-hidden="true"><House /></span><strong>民宿预约系统</strong></RouterLink
    >
    <section class="login-story" aria-label="住宿预订服务">
      <div class="login-photo">
        <img src="/attractions/sight-canton-tower.jpg" alt="广州塔与珠江城市风景" />
      </div>
      <p class="image-credit">
        <a
          href="https://commons.wikimedia.org/wiki/File:Canton_Tower_20241027.jpg"
          target="_blank"
          rel="noreferrer"
          >广州塔 · Tim Wu</a
        >
        /
        <a href="https://creativecommons.org/licenses/by-sa/4.0/" target="_blank" rel="noreferrer"
          >CC BY-SA 4.0</a
        >
        · 图片已裁切
      </p>
      <div class="login-story-copy">
        <h1>住宿、行程，一处管理</h1>
        <p>登录后预订酒店与民宿，收藏心仪住处，随时查看订单。</p>
        <RouterLink to="/search">先浏览住宿 →</RouterLink>
      </div>
    </section>
    <section class="login-panel" aria-labelledby="login-title">
      <div v-if="entrance === USER_ROLES.USER" class="mode-tabs">
        <button
          :class="{ active: mode === 'login' }"
          :aria-pressed="mode === 'login'"
          :disabled="loading"
          type="button"
          @click="switchMode('login')"
        >
          登录</button
        ><button
          :class="{ active: mode === 'register' }"
          :aria-pressed="mode === 'register'"
          :disabled="loading"
          type="button"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>
      <h2 id="login-title" aria-live="polite">
        {{ mode === 'register' ? '创建住客账号' : entranceTitle }}
      </h2>
      <p>
        {{
          entrance === USER_ROLES.USER
            ? '登录后可预订房源、查看订单和管理行程。'
            : entrance === USER_ROLES.MERCHANT
              ? '使用商家账号，管理房源、房态与住客订单。'
              : '使用管理员账号，进入平台管理中心。'
        }}
      </p>
      <form :aria-busy="loading" @submit.prevent="submit" @input="formError = ''">
        <label v-if="mode === 'register'"
          >用户名<input
            v-model="form.name"
            autocomplete="nickname"
            maxlength="64"
            placeholder="请输入用户名"
            :disabled="loading"
        /></label>
        <label
          >手机号<input
            v-model.trim="form.phone"
            type="tel"
            autocomplete="username"
            inputmode="numeric"
            maxlength="11"
            placeholder="请输入手机号"
            :disabled="loading"
        /></label>
        <label v-if="mode === 'register'"
          >邮箱<input
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            maxlength="128"
            placeholder="请输入邮箱地址"
            :disabled="loading"
        /></label>
        <div class="password-field">
          <label for="login-password">密码</label>
          <div class="password-input">
            <input
              id="login-password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              :autocomplete="mode === 'register' ? 'new-password' : 'current-password'"
              placeholder="请输入 6–32 位密码"
              maxlength="32"
              :disabled="loading"
            />
            <button
              type="button"
              :aria-pressed="showPassword"
              :disabled="loading"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '隐藏' : '显示' }}密码
            </button>
          </div>
        </div>
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <button class="login-button" type="submit" :disabled="loading">
          {{ loading ? '正在提交…' : mode === 'register' ? '注册并登录' : '登录' }}
        </button>
      </form>
      <button
        v-if="useMock"
        class="demo-entry"
        type="button"
        @click="loginAs(accounts.find((item) => item.role === entrance))"
      >
        本地预览入口
      </button>
      <nav class="entrance-links" aria-label="其他登录入口">
        <template v-if="entrance === USER_ROLES.USER">
          <button type="button" :disabled="loading" @click="switchEntrance(USER_ROLES.MERCHANT)">
            商家登录
          </button>
          <span aria-hidden="true">·</span>
          <button type="button" :disabled="loading" @click="switchEntrance(USER_ROLES.ADMIN)">
            管理员登录
          </button>
        </template>
        <template v-else>
          <button type="button" :disabled="loading" @click="switchEntrance(USER_ROLES.USER)">
            返回用户登录
          </button>
          <span aria-hidden="true">·</span>
          <button
            type="button"
            :disabled="loading"
            @click="
              switchEntrance(
                entrance === USER_ROLES.MERCHANT ? USER_ROLES.ADMIN : USER_ROLES.MERCHANT,
              )
            "
          >
            {{ entrance === USER_ROLES.MERCHANT ? '管理员登录' : '商家登录' }}
          </button>
        </template>
      </nav>
      <p v-if="useMock" class="security-note">当前为本地预览环境，请勿填写真实个人信息。</p>
      <RouterLink class="back-home" to="/">返回首页</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 440px;
  align-items: center;
  gap: 80px;
  width: min(100% - 80px, 1120px);
  min-height: 100vh;
  margin: auto;
  padding: 120px 0 64px;
  color: var(--ink);
}
.brand {
  position: absolute;
  top: 32px;
  left: 0;
  display: flex;
  gap: 10px;
  align-items: center;
  color: var(--ink);
  text-decoration: none;
  font-size: 20px;
}
.brand span {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--brand);
  color: white;
  font-size: 26px;
}
.brand svg {
  width: 23px;
  height: 23px;
}
.login-photo {
  height: 310px;
  border-radius: 12px;
  overflow: hidden;
  background: #e7ebf0;
}
.login-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.login-story-copy {
  padding: 24px 0;
}
.login-story .image-credit {
  font-size: 11px;
  margin: 8px 0 0;
  color: var(--muted);
}
.image-credit a {
  color: inherit;
}
.login-story h1 {
  font-size: 28px;
  font-weight: 650;
  margin: 0 0 12px;
}
.login-story p {
  max-width: 34em;
  color: var(--muted);
  font-size: 15px;
  margin: 0 0 20px;
}
.login-story a {
  color: var(--brand);
  text-decoration: none;
}
.login-panel {
  padding: 32px;
  background: white;
  border: 1px solid var(--line);
  border-radius: 12px;
}
.mode-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 24px;
}
.mode-tabs button {
  min-height: 44px;
  padding: 0 0 12px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--muted);
  font-size: 16px;
  cursor: pointer;
}
.mode-tabs button.active {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 650;
}
.login-panel h2 {
  font-size: 25px;
  margin: 0 0 8px;
}
.login-panel > p {
  color: var(--muted);
  font-size: 13px;
  margin: 0 0 24px;
}
form {
  display: grid;
  gap: 18px;
}
label {
  display: grid;
  gap: 8px;
  font-size: 14px;
  color: var(--ink);
}
input {
  width: 100%;
  min-height: 46px;
  padding: 10px 12px;
  border: 1px solid #aeb8c6;
  border-radius: 6px;
  background: white;
  font-size: 14px;
}
input:hover {
  border-color: var(--brand);
}
.login-button {
  min-height: 46px;
  border: 0;
  border-radius: 6px;
  color: white;
  background: var(--brand);
  cursor: pointer;
  font-size: 16px;
  font-weight: 650;
}
.login-button:hover {
  background: var(--brand-hover);
}
.form-error {
  margin: 0;
  padding: 10px 12px;
  color: #b42318;
  background: #fff3f1;
  border-radius: 6px;
  font-size: 13px;
}
.password-field {
  display: grid;
  gap: 8px;
}
.password-input {
  position: relative;
}
.password-input input {
  padding-right: 90px;
}
.password-input button {
  position: absolute;
  right: 6px;
  top: 2px;
  min-height: 42px;
  padding: 0 10px;
  border: 0;
  background: transparent;
  color: var(--brand);
  cursor: pointer;
}
.entrance-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}
.entrance-links button {
  min-height: 44px;
  border: 0;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font-size: 13px;
}
.entrance-links button:hover {
  color: var(--brand);
  text-decoration: underline;
  text-underline-offset: 4px;
}
.demo-entry {
  display: block;
  min-height: 44px;
  margin: 12px auto 0;
  border: 0;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font-size: 13px;
}
.login-panel :is(button, input):disabled {
  cursor: wait;
  opacity: 0.65;
}
.entrance-links button.active {
  color: var(--brand);
  font-weight: 650;
}
.back-home {
  display: block;
  margin-top: 24px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
.demo-divider {
  margin-top: 24px;
  font-size: 12px;
  color: var(--muted);
}
.demo-accounts {
  display: flex;
  gap: 8px;
  margin: 12px 0;
}
.demo-accounts button {
  display: grid;
  gap: 4px;
  flex: 1;
  padding: 10px 6px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: white;
  cursor: pointer;
}
.demo-accounts span,
.security-note {
  font-size: 12px;
  color: var(--muted);
}
@media (max-width: 900px) {
  .login-page {
    gap: 36px;
    grid-template-columns: 1fr 400px;
    width: calc(100% - 48px);
  }
  .login-photo {
    height: 260px;
  }
}
@media (max-width: 700px) {
  .login-page {
    display: block;
    min-height: 100vh;
    max-width: 440px;
    width: calc(100% - 32px);
    padding-top: 100px;
  }
  .login-story {
    display: none;
  }
  .login-panel {
    padding: 24px;
  }
  .brand {
    top: 24px;
  }
}
</style>
