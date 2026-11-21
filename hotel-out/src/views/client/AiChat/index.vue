<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowRight, ChatDotRound, Delete, Position, RefreshRight } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import NavHeader from '@/components/common/NavHeader.vue'
import { getAiStatus, searchWithAi } from '@/api/modules/ai'
import { normalizeHotel } from '@/services/dataSource'
import { useAppStore } from '@/stores'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const input = ref('')
const status = ref('checking')
const mode = ref('auto')
const busy = ref(false)
const error = ref('')
const messages = ref([])
const resultByMessage = ref({})
const timeline = ref(null)
const modeMenu = ref(null)
const requestId = ref(0)
const failedTurnId = ref('')
const modeOptions = [
  { id: 'auto', label: '自动模式', description: '根据问题复杂度选择回答方式' },
  { id: 'deep', label: '深度思考', description: '适合多条件比较与行程规划' },
  { id: 'quick', label: '快速回答', description: '适合查价、设施与入住规则' },
]
const suggestions = computed(() => {
  const city = appStore.selectedCity || '广州'
  return [
    `${city}周末适合家庭入住的民宿`,
    `${city}每晚 500 元以内、靠近地铁的房源`,
    `${city}有什么带停车位的住宿`,
  ]
})
const currentMode = computed(
  () => modeOptions.find((item) => item.id === mode.value) || modeOptions[0],
)
const conversationKey = computed(() => `hotel_ai_chat_${appStore.selectedCity || 'all'}`)
const canSend = computed(() => !busy.value && status.value === 'ready' && input.value.trim())

function save() {
  try {
    sessionStorage.setItem(
      conversationKey.value,
      JSON.stringify({ messages: messages.value, resultByMessage: resultByMessage.value }),
    )
  } catch {
    /* 会话存储不可用时仍保留当前页面对话 */
  }
}
function restore() {
  try {
    const stored = JSON.parse(sessionStorage.getItem(conversationKey.value) || '{}')
    messages.value = Array.isArray(stored.messages) ? stored.messages : []
    resultByMessage.value =
      stored.resultByMessage && typeof stored.resultByMessage === 'object'
        ? stored.resultByMessage
        : {}
  } catch {
    messages.value = []
    resultByMessage.value = {}
  }
}
async function scrollLatest() {
  await nextTick()
  if (timeline.value) timeline.value.scrollTop = timeline.value.scrollHeight
}
async function checkStatus() {
  status.value = 'checking'
  try {
    const data = await getAiStatus()
    status.value = data.mock ? 'mock' : data.enabled ? 'ready' : 'disabled'
  } catch {
    status.value = 'offline'
  }
}
async function send(prefill = '') {
  if (busy.value || status.value !== 'ready') return
  if (typeof prefill !== 'string') prefill = ''
  const content = (prefill || input.value).trim()
  if (!content) {
    error.value = '请先输入想了解的目的地或住宿条件'
    return
  }
  const id = `turn-${Date.now()}-${requestId.value + 1}`
  const retryMessage = [...messages.value]
    .reverse()
    .find(
      (item) => item.id === failedTurnId.value && item.role === 'user' && item.content === content,
    )
  const turnId = retryMessage?.id || id
  const current = ++requestId.value
  const selectedMode = mode.value
  if (!retryMessage) {
    failedTurnId.value = ''
    messages.value.push({ id: turnId, role: 'user', content, createdAt: new Date().toISOString() })
  }
  input.value = ''
  error.value = ''
  busy.value = true
  await scrollLatest()
  try {
    // The API accepts at most six previous turns. Exclude the current user
    // message so a retry or a new send cannot count it twice as history.
    const history = messages.value
      .filter((item) => item.id !== turnId)
      .slice(-6)
      .map(({ role, content: text }) => ({ role, content: text.slice(0, 1000) }))
    const response = await searchWithAi({
      message: content,
      mode: selectedMode,
      city: appStore.selectedCity,
      history,
    })
    if (current !== requestId.value) return
    messages.value.push({
      id: `${turnId}-answer`,
      role: 'assistant',
      content: response.message,
      createdAt: new Date().toISOString(),
    })
    failedTurnId.value = ''
    resultByMessage.value[turnId] = response
    save()
    await scrollLatest()
  } catch (cause) {
    if (current === requestId.value) {
      error.value = cause.message || '暂时没有得到回答，请稍后重试'
      failedTurnId.value = turnId
      input.value = content
    }
  } finally {
    if (current === requestId.value) busy.value = false
  }
}
function clearConversation() {
  requestId.value++
  messages.value = []
  resultByMessage.value = {}
  failedTurnId.value = ''
  error.value = ''
  input.value = ''
  try {
    sessionStorage.removeItem(conversationKey.value)
  } catch {
    /* ignore */
  }
}
function selectMode(id) {
  mode.value = id
  if (modeMenu.value) modeMenu.value.open = false
}
function handleModeOutside(event) {
  if (modeMenu.value && !modeMenu.value.contains(event.target)) modeMenu.value.open = false
}
function statusLabel(value) {
  return {
    checking: '正在连接',
    ready: '可以开始',
    disabled: 'AI 未启用',
    offline: '暂时离线',
    mock: '演示模式',
  }[value]
}
function hotels(response) {
  return (response?.hotels || []).map((hotel) => normalizeHotel(hotel))
}
function filters(response) {
  const value = response?.filters || {}
  return [
    value.city,
    value.checkIn && `${value.checkIn} 至 ${value.checkOut}`,
    value.guests && `${value.guests} 位入住`,
    value.maxPrice && `每晚 ¥${value.maxPrice}`,
    value.facility,
  ].filter(Boolean)
}
function searchQuery(response) {
  return Object.fromEntries(
    Object.entries(response?.filters || {}).filter(
      ([key, value]) =>
        !['pageSize', 'pageNum'].includes(key) &&
        value !== null &&
        value !== undefined &&
        value !== '',
    ),
  )
}
function answerFor(message) {
  return resultByMessage.value[message.id]
}
function onHomeQuestion() {
  const query = String(route.query.q || '').trim()
  if (!query) return
  if (route.query.city) appStore.setSelectedCity(String(route.query.city))
  mode.value = modeOptions.some((item) => item.id === route.query.mode) ? route.query.mode : 'auto'
  router.replace({ name: 'ai-chat', query: { ...route.query, q: undefined, mode: undefined } })
  return (async () => {
    if (status.value !== 'ready') await checkStatus()
    await send(query)
  })()
}
watch(conversationKey, () => {
  restore()
  scrollLatest()
})
onMounted(() => {
  document.addEventListener('pointerdown', handleModeOutside)
  restore()
  checkStatus().then(onHomeQuestion)
})
onBeforeUnmount(() => document.removeEventListener('pointerdown', handleModeOutside))
</script>

<template>
  <div class="ai-chat-page">
    <NavHeader />
    <main class="ai-chat-main">
      <header class="ai-chat-head">
        <div>
          <p class="eyebrow">TRIP ASSISTANT</p>
          <h1>AI 旅行顾问</h1>
          <p>把目的地、预算和入住偏好告诉我，一起找到合适的住处。</p>
        </div>
        <button
          class="clear-button"
          type="button"
          :disabled="busy || !messages.length"
          @click="clearConversation"
        >
          <Delete />清空对话
        </button>
      </header>
      <section class="ai-chat-shell" :aria-busy="busy">
        <div class="ai-chat-status" :class="status">
          <span></span>{{ statusLabel(status)
          }}<button v-if="status === 'offline'" type="button" @click="checkStatus">
            <RefreshRight />重试连接
          </button>
        </div>
        <div ref="timeline" class="ai-chat-timeline" role="log" aria-live="polite">
          <div v-if="!messages.length" class="ai-chat-empty">
            <ChatDotRound />
            <h2>从一句话开始你的旅程</h2>
            <p>可以问我住哪里、怎么选房型，或帮你比较几个住宿方案。</p>
            <div class="ai-suggestion-row">
              <button
                v-for="suggestion in suggestions"
                :key="suggestion"
                type="button"
                @click="input = suggestion"
              >
                {{ suggestion }}<ArrowRight />
              </button>
            </div>
          </div>
          <template v-for="message in messages" :key="message.id">
            <article class="ai-message" :class="message.role">
              <div class="ai-message-label">
                {{ message.role === 'user' ? '你' : 'AI 旅行顾问'
                }}<time>{{ dayjs(message.createdAt).format('HH:mm') }}</time>
              </div>
              <p>{{ message.content }}</p>
            </article>
            <div v-if="message.role === 'user' && answerFor(message)" class="ai-answer-result">
              <div v-if="filters(answerFor(message)).length" class="ai-filter-tags">
                <span v-for="tag in filters(answerFor(message))" :key="tag">{{ tag }}</span>
              </div>
              <div v-if="hotels(answerFor(message)).length" class="ai-result-grid">
                <RouterLink
                  v-for="hotel in hotels(answerFor(message))"
                  :key="hotel.id"
                  :to="{
                    name: 'hotel-detail',
                    params: { id: hotel.id },
                    query: searchQuery(answerFor(message)),
                  }"
                  class="ai-result-card"
                  ><img v-if="hotel.image" :src="hotel.image" :alt="hotel.name" loading="lazy" />
                  <div>
                    <strong>{{ hotel.name }}</strong
                    ><span>{{ hotel.city }} · {{ hotel.district || hotel.address }}</span
                    ><b v-if="hotel.price > 0">¥{{ hotel.price }}<small>起 / 晚</small></b>
                  </div></RouterLink
                >
              </div>
              <RouterLink
                v-if="answerFor(message).filters?.city"
                class="ai-result-link"
                :to="{ name: 'client-search', query: searchQuery(answerFor(message)) }"
                >查看全部房源 <ArrowRight
              /></RouterLink>
            </div>
          </template>
          <div v-if="busy" class="ai-message assistant">
            <div class="ai-message-label">AI 旅行顾问</div>
            <p class="ai-thinking"><i></i><i></i><i></i>正在整理适合你的房源</p>
          </div>
        </div>
        <p v-if="status === 'disabled' || status === 'mock'" class="ai-chat-notice">
          AI 服务当前未启用，可以先使用酒店搜索。<RouterLink to="/search">去搜索房源</RouterLink>
        </p>
        <p v-if="error" class="ai-chat-error" role="alert">
          {{ error }} <button type="button" @click="send">重试</button>
        </p>
        <form class="ai-chat-composer" @submit.prevent="send">
          <label for="ai-chat-input">继续提问</label
          ><textarea
            id="ai-chat-input"
            v-model="input"
            :disabled="busy || status !== 'ready'"
            maxlength="1000"
            rows="2"
            placeholder="例如：帮我比较两家适合带孩子入住的民宿"
            @keydown.ctrl.enter.prevent="send"
            @keydown.meta.enter.prevent="send"
          ></textarea>
          <div class="ai-chat-composer-footer">
            <details ref="modeMenu">
              <summary>{{ currentMode.label }}<small>⌄</small></summary>
              <div class="mode-options">
                <button
                  v-for="item in modeOptions"
                  :key="item.id"
                  type="button"
                  :class="{ selected: mode === item.id }"
                  @click="selectMode(item.id)"
                >
                  <strong>{{ item.label }}</strong
                  ><span>{{ item.description }}</span>
                </button>
              </div>
            </details>
            <span>{{ input.length }}/1000 · Ctrl / ⌘ + Enter 发送</span
            ><button class="ai-send-button" type="submit" :disabled="!canSend">
              <Position />{{ busy ? '思考中…' : '发送' }}
            </button>
          </div>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.ai-chat-page {
  min-height: 100vh;
  background: #f5f6f8;
  color: var(--ink);
}
.ai-chat-main {
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 34px 0 48px;
}
.ai-chat-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}
.eyebrow {
  margin: 0 0 8px;
  color: #7563a7;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}
.ai-chat-head h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: -0.03em;
}
.ai-chat-head p:last-child {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
}
.clear-button {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid var(--line);
  border-radius: 6px;
  color: var(--muted);
  background: #fff;
  cursor: pointer;
}
.clear-button svg {
  width: 16px;
}
.ai-chat-shell {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 680px;
  height: calc(100dvh - 190px);
  max-height: 820px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 16px 40px rgb(16 42 67 / 6%);
}
.ai-chat-status {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 22px;
  border-bottom: 1px solid var(--line);
  color: var(--muted);
  font-size: 12px;
}
.ai-chat-status > span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #aeb5c3;
}
.ai-chat-status.ready > span {
  background: #32a477;
}
.ai-chat-status.disabled > span,
.ai-chat-status.offline > span {
  background: #d46c54;
}
.ai-chat-status button {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  border: 0;
  color: var(--brand);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
}
.ai-chat-status button svg {
  width: 14px;
}
.ai-chat-timeline {
  min-height: 0;
  overflow-y: auto;
  padding: 26px 30px;
  background: #f7f8fa;
}
.ai-chat-empty {
  display: flex;
  min-height: 390px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  text-align: center;
  color: var(--muted);
}
.ai-chat-empty > svg {
  width: 42px;
  height: 42px;
  color: var(--brand);
}
.ai-chat-empty h2 {
  margin: 0;
  color: var(--ink);
  font-size: 20px;
}
.ai-chat-empty p {
  margin: 0;
  font-size: 13px;
}
.ai-suggestion-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  max-width: 690px;
  margin-top: 12px;
}
.ai-suggestion-row button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 12px;
  border: 1px solid #e1dcee;
  border-radius: 6px;
  color: #5f527d;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
}
.ai-suggestion-row button svg {
  width: 14px;
}
.ai-message {
  max-width: 76%;
  margin: 0 0 20px;
}
.ai-message.user {
  margin-left: auto;
}
.ai-message-label {
  display: flex;
  gap: 9px;
  margin: 0 4px 6px;
  color: var(--muted);
  font-size: 11px;
}
.ai-message.user .ai-message-label {
  justify-content: flex-end;
}
.ai-message p {
  margin: 0;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: 4px 10px 10px 10px;
  background: #fff;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 14px;
  line-height: 1.8;
}
.ai-message.user p {
  border-color: var(--brand);
  border-radius: 10px 4px 10px 10px;
  background: var(--brand);
  color: #fff;
}
.ai-thinking {
  display: flex !important;
  align-items: center;
  gap: 5px;
  color: var(--muted) !important;
}
.ai-thinking i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--brand);
  animation: pulse 1s infinite alternate;
}
.ai-thinking i:nth-child(2) {
  animation-delay: 0.2s;
}
.ai-thinking i:nth-child(3) {
  animation-delay: 0.4s;
}
.ai-answer-result {
  margin: -8px 0 22px 4px;
}
.ai-filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.ai-filter-tags span {
  padding: 5px 8px;
  border-radius: 4px;
  background: #eeeaf7;
  color: #68589b;
  font-size: 11px;
}
.ai-result-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.ai-result-card {
  display: block;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fff;
  color: inherit;
  text-decoration: none;
}
.ai-result-card img {
  display: block;
  width: 100%;
  height: 92px;
  object-fit: cover;
}
.ai-result-card > div {
  display: grid;
  gap: 4px;
  padding: 10px;
}
.ai-result-card strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.ai-result-card span {
  overflow: hidden;
  color: var(--muted);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
}
.ai-result-card b {
  color: #b84d20;
  font-size: 14px;
}
.ai-result-card small {
  margin-left: 3px;
  color: var(--muted);
  font-size: 10px;
  font-weight: 400;
}
.ai-result-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  color: var(--brand);
  font-size: 12px;
}
.ai-result-link svg {
  width: 13px;
}
.ai-chat-notice,
.ai-chat-error {
  margin: 0;
  padding: 10px 22px;
  font-size: 12px;
  line-height: 1.7;
}
.ai-chat-notice {
  color: var(--muted);
  background: #faf9fd;
}
.ai-chat-notice a {
  margin-left: 8px;
  color: var(--brand);
}
.ai-chat-error {
  color: #b42318;
  background: #fff2f0;
}
.ai-chat-error button {
  margin-left: 8px;
  border: 0;
  color: #b42318;
  background: transparent;
  text-decoration: underline;
  cursor: pointer;
}
.ai-chat-composer {
  padding: 16px 22px;
  border-top: 1px solid var(--line);
  background: #fff;
}
.ai-chat-composer > label {
  display: block;
  margin-bottom: 7px;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}
.ai-chat-composer textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 64px;
  resize: vertical;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  font: inherit;
  font-size: 14px;
  line-height: 1.7;
}
.ai-chat-composer textarea:focus {
  outline: 2px solid #e5def4;
  border-color: var(--brand);
}
.ai-chat-composer-footer {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  color: var(--muted);
  font-size: 11px;
}
.ai-chat-composer-footer > span {
  margin-left: auto;
}
.ai-chat-composer-footer details {
  position: relative;
}
.ai-chat-composer-footer summary {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 6px;
  color: #5f527d;
  font-size: 12px;
  cursor: pointer;
  list-style: none;
}
.ai-chat-composer-footer summary small {
  font-size: 14px;
}
.mode-options {
  position: absolute;
  bottom: 48px;
  left: 0;
  z-index: 3;
  width: 250px;
  padding: 7px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(16 42 67 / 12%);
}
.mode-options button {
  display: grid;
  width: 100%;
  gap: 3px;
  padding: 9px;
  border: 0;
  border-radius: 5px;
  text-align: left;
  background: transparent;
  cursor: pointer;
}
.mode-options button.selected,
.mode-options button:hover {
  background: #f3effc;
}
.mode-options strong {
  color: var(--ink);
  font-size: 12px;
}
.mode-options span {
  color: var(--muted);
  font-size: 10px;
}
.ai-send-button {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  padding: 0 18px;
  border: 0;
  border-radius: 6px;
  background: var(--brand);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
}
.ai-send-button svg {
  width: 16px;
}
.ai-send-button:disabled,
.clear-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
@keyframes pulse {
  to {
    opacity: 0.35;
    transform: translateY(-2px);
  }
}
@media (max-width: 768px), (hover: none) and (pointer: coarse) and (max-width: 1024px) {
  .ai-chat-main {
    width: calc(100% - 24px);
    padding: 18px 0 88px;
  }
  .ai-chat-head {
    align-items: start;
  }
  .ai-chat-head h1 {
    font-size: 24px;
  }
  .ai-chat-head p:last-child {
    font-size: 12px;
    line-height: 1.6;
  }
  .clear-button {
    padding: 0 10px;
    font-size: 11px;
  }
  .ai-chat-shell {
    height: calc(100dvh - 190px - env(safe-area-inset-bottom, 0px));
    min-height: 510px;
  }
  .ai-chat-timeline {
    padding: 18px 12px;
  }
  .ai-message {
    max-width: 88%;
  }
  .ai-result-grid {
    grid-template-columns: 1fr;
  }
  .ai-result-card {
    display: flex;
  }
  .ai-result-card img {
    width: 92px;
    height: 92px;
    flex: none;
  }
  .ai-chat-composer {
    padding: 12px;
  }
  .ai-chat-composer textarea {
    font-size: 16px;
  }
  .ai-chat-composer-footer {
    flex-wrap: wrap;
  }
  .ai-chat-composer-footer > span {
    width: 100%;
    order: 3;
    margin-left: 0;
  }
  .ai-send-button {
    margin-left: auto;
  }
  .mode-options {
    bottom: 48px;
    max-width: calc(100vw - 48px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .ai-thinking i {
    animation: none;
  }
}
</style>
