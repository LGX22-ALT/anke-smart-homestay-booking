<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ArrowRight, ChatDotRound, Position, RefreshRight } from '@element-plus/icons-vue'
import { getAiStatus } from '@/api/modules/ai'
import { normalizeHotel } from '@/services/dataSource'
import { useAppStore } from '@/stores'
import { useRouter } from 'vue-router'

const appStore = useAppStore()
const router = useRouter()
const question = ref('')
const status = ref('checking')
const busy = ref(false)
const error = ref('')
const turns = ref([])
const result = ref(null)
const answerMode = ref('auto')
const modeMenu = ref(null)
const answerModes = [
  { id: 'auto', label: '自动模式', icon: '✧', description: '按问题复杂度选择回答方式' },
  { id: 'deep', label: '深思考', icon: '◈', description: '复杂问题拆解、方案比较与平台检索' },
  { id: 'quick', label: '快回答', icon: 'ϟ', description: '简洁直接，适合查询、翻译与计算' },
]
const currentMode = computed(() => answerModes.find((item) => item.id === answerMode.value))
function closeModeMenu(event) {
  if (modeMenu.value && !modeMenu.value.contains(event.target)) modeMenu.value.open = false
}
onMounted(() => document.addEventListener('pointerdown', closeModeMenu))
onUnmounted(() => document.removeEventListener('pointerdown', closeModeMenu))
function chooseMode(id) {
  answerMode.value = id
  modeMenu.value.open = false
}
const city = computed(() => appStore.selectedCity || '广州')
const suggestions = computed(() => [
  `${city.value}每晚 500 元以内，适合两个人住的民宿`,
  `想去${city.value}，帮我找带停车场的酒店`,
  `${city.value}有没有适合一家三口的住宿？`,
])
const statusText = computed(
  () =>
    ({
      checking: '正在连接',
      ready: '可以开始提问',
      disabled: '暂未启用',
      offline: '暂时无法连接',
      mock: '服务未连接',
    })[status.value],
)
const filters = computed(() => result.value?.filters || {})
const searchQuery = computed(() =>
  Object.fromEntries(
    Object.entries(filters.value).filter(
      ([key, value]) =>
        !['pageSize', 'pageNum'].includes(key) &&
        value !== null &&
        value !== '' &&
        value !== undefined,
    ),
  ),
)
const foundHotels = computed(() =>
  (result.value?.hotels || []).map((hotel) => normalizeHotel(hotel)),
)
const conditionLabels = computed(() =>
  [
    filters.value.city,
    filters.value.checkIn && `${filters.value.checkIn} → ${filters.value.checkOut}`,
    filters.value.guests && `${filters.value.guests} 人入住`,
    filters.value.maxPrice != null && `每晚 ≤ ¥${filters.value.maxPrice}`,
    filters.value.facility,
    filters.value.keyword,
  ].filter(Boolean),
)

async function checkStatus() {
  status.value = 'checking'
  try {
    const data = await getAiStatus()
    status.value = data.mock ? 'mock' : data.enabled ? 'ready' : 'disabled'
  } catch {
    status.value = 'offline'
  }
}
async function send() {
  if (busy.value) return
  const message = question.value.trim()
  if (!message) {
    error.value = '请说说你想去哪里、住什么样的房子。'
    return
  }
  if (status.value !== 'ready') return
  router.push({
    name: 'ai-chat',
    query: { q: message, mode: answerMode.value, city: appStore.selectedCity || undefined },
  })
}
function reset() {
  busy.value = false
  turns.value = []
  result.value = null
  error.value = ''
  question.value = ''
}
watch(() => appStore.selectedCity, reset)
onMounted(checkStatus)
</script>

<template>
  <section class="ai-stay" aria-labelledby="ai-stay-title" :aria-busy="busy">
    <div class="assistant-intro">
      <span class="assistant-label"><span></span> 住得合心意 · AI 住宿助手</span>
      <h1 id="ai-stay-title">找酒店 · 选民宿<em>一句话帮你找</em></h1>
      <RouterLink
        class="manual-search"
        :to="{ name: 'client-search', query: { city: appStore.selectedCity || undefined } }"
        >也可以自己搜酒店 <ArrowRight
      /></RouterLink>
    </div>
    <div class="assistant-workspace">
      <form class="assistant-composer" @submit.prevent="send">
        <div class="composer-heading">
          <span><ChatDotRound /> 说说你的住宿计划</span
          ><small :class="{ connected: status === 'ready' }"><i></i>{{ statusText }}</small>
        </div>
        <label class="sr-only" for="ai-stay-question">住宿需求</label>
        <textarea
          id="ai-stay-question"
          v-model="question"
          :disabled="busy"
          maxlength="1000"
          rows="2"
          :placeholder="`例如：下周六去${city}住两晚，两个人，每晚 500 元以内，想离地铁近一点。`"
          @keydown.ctrl.enter.prevent="send"
          @keydown.meta.enter.prevent="send"
        ></textarea>
        <div class="composer-footer">
          <details
            ref="modeMenu"
            class="answer-mode-menu"
            @focusout="
              (event) => {
                if (!event.currentTarget.contains(event.relatedTarget))
                  event.currentTarget.open = false
              }
            "
          >
            <summary :aria-label="`选择回答模式，当前${currentMode.label}`">
              {{ currentMode.icon }} {{ currentMode.label }} <small>⌃</small>
            </summary>
            <div class="answer-mode-popover">
              <strong>选择回答模式</strong
              ><button
                v-for="item in answerModes"
                :key="item.id"
                type="button"
                :disabled="busy"
                :aria-pressed="answerMode === item.id"
                :class="{ selected: answerMode === item.id }"
                @click="chooseMode(item.id)"
              >
                <i>{{ item.icon }}</i
                ><span
                  ><b>{{ item.label }}</b
                  ><small>{{ item.description }}</small></span
                ><em v-if="answerMode === item.id">✓</em>
              </button>
            </div>
          </details>
          <button type="submit" :disabled="busy || status !== 'ready'">
            <span>{{ busy ? '正在找合适的住宿…' : '帮我找住宿' }}</span
            ><Position v-if="!busy" /><span v-else class="loading-dot"></span>
          </button>
        </div>
      </form>
      <p v-if="status === 'disabled' || status === 'mock'" class="assistant-notice">
        {{
          status === 'mock'
            ? '当前环境未连接 AI 服务，可使用酒店搜索筛选住宿。'
            : 'AI 搜索暂未开放，可使用酒店搜索筛选住宿。'
        }}<RouterLink to="/search">先逛逛酒店</RouterLink>
      </p>
      <p v-else-if="status === 'offline'" class="assistant-notice">
        助手暂时无法连接，酒店搜索仍可使用。<button type="button" @click="checkStatus">
          重新连接
        </button>
      </p>
      <p v-if="error" role="alert" class="assistant-error">
        {{ error }} <RouterLink to="/search">使用酒店搜索</RouterLink>
      </p>
      <div class="suggestion-list" aria-label="试试这样问">
        <span>灵感从这里开始</span
        ><button
          v-for="suggestion in suggestions"
          :key="suggestion"
          type="button"
          :disabled="busy"
          @click="question = suggestion"
        >
          {{ suggestion }}<ArrowRight />
        </button>
      </div>
    </div>
    <div v-if="result" class="assistant-results" aria-live="polite">
      <div class="result-heading">
        <span><ChatDotRound /> 你的住宿清单</span
        ><button type="button" :disabled="busy" @click="reset"><RefreshRight /> 开始新需求</button>
      </div>
      <p class="last-question">你：{{ turns[turns.length - 2]?.content }}</p>
      <p class="result-message">{{ result.message }}</p>
      <div v-if="conditionLabels.length" class="condition-list">
        <span v-for="label in conditionLabels" :key="label">{{ label }}</span>
      </div>
      <div v-if="foundHotels.length" class="ai-hotel-grid">
        <RouterLink
          v-for="hotel in foundHotels"
          :key="hotel.id"
          class="ai-hotel"
          :to="{ name: 'hotel-detail', params: { id: hotel.id }, query: searchQuery }"
          ><img v-if="hotel.image" :src="hotel.image" :alt="hotel.name" loading="lazy" />
          <div>
            <h3>{{ hotel.name }}</h3>
            <p>{{ hotel.city }} · {{ hotel.district || hotel.address }}</p>
            <div>
              <strong v-if="hotel.price > 0">¥{{ hotel.price }}<small>起 / 晚</small></strong
              ><span v-else>查看价格</span><b>选房型 <ArrowRight /></b>
            </div></div
        ></RouterLink>
      </div>
      <div class="result-footer">
        <span>价格与剩余房量以房型页面为准，预订需登录并确认。</span
        ><RouterLink v-if="filters.city" :to="{ name: 'client-search', query: searchQuery }"
          >查看全部结果 <ArrowRight
        /></RouterLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.answer-mode-menu {
  position: relative;
  z-index: 5;
  flex: none;
}
.answer-mode-menu summary {
  display: flex;
  gap: 7px;
  align-items: center;
  padding: 10px 13px;
  border: 1px solid #e0dcea;
  border-radius: 22px;
  background: #fff;
  color: #43365e;
  font-size: 12px;
  cursor: pointer;
  list-style: none;
}
.answer-mode-menu summary::-webkit-details-marker {
  display: none;
}
.answer-mode-popover {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 0;
  width: 276px;
  max-width: calc(100vw - 65px);
  padding: 10px;
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #ece8f4;
  border-radius: 16px;
  box-shadow: 0 8px 32px #25213a20;
}
.answer-mode-popover > strong {
  display: block;
  padding: 6px 8px 10px;
  font-size: 12px;
}
.composer-footer .answer-mode-popover button {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 10px 9px;
  min-height: 54px;
  margin: 0;
  background: transparent;
  color: #202938;
  border: 0;
  border-radius: 9px;
  box-shadow: none;
  text-align: left;
}
.composer-footer .answer-mode-popover button.selected {
  background: #f3effc;
  color: #6246a0;
}
.answer-mode-popover button > span {
  display: grid;
  gap: 4px;
  flex: 1;
}
.answer-mode-popover b {
  font-size: 13px;
}
.answer-mode-popover small {
  font-size: 10px;
  font-weight: 400;
  color: #8790a0;
}
.answer-mode-popover i {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  background: #f5f4f9;
  border-radius: 8px;
  font-size: 21px;
  font-style: normal;
}
.answer-mode-popover em {
  font-style: normal;
  color: #7151c8;
}
.ai-stay {
  --ink: #20314d;
  --violet: #6750b5;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 14px;
  width: min(100%, 1000px);
  box-sizing: border-box;
  padding: 22px 26px;
  margin: 0 auto 30px;
  border: 1px solid #e5e9f2;
  border-radius: 18px;
  background: #f4f6fc;
  color: var(--ink);
  font-family: var(--font-ui);
}
.assistant-intro {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 16px;
  align-items: center;
}
.assistant-label {
  grid-column: 1 / -1;
  display: flex;
  gap: 8px;
  align-items: center;
  color: #685a9c;
  font-size: 11px;
  font-weight: 600;
}
.assistant-label > span {
  width: 9px;
  height: 9px;
  border-radius: 3px;
  background: #8571c7;
  box-shadow: 3px 3px 0 #d7ccf1;
  transform: rotate(-12deg);
}
.assistant-intro h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.4;
  letter-spacing: -0.045em;
  font-weight: 750;
}
.assistant-intro em {
  font-style: normal;
  color: var(--violet);
}
.manual-search {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #514478;
  font-size: 12px;
  text-decoration: none;
  margin: 0;
}
.manual-search svg {
  width: 13px;
}
.assistant-workspace {
  align-self: center;
  min-width: 0;
}
.assistant-composer {
  padding: 14px 18px;
  border: 1px solid #d5cbea;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 35px #4032640a;
}
.composer-heading {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
}
.composer-heading > span {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 600;
  color: #5b4b87;
}
.composer-heading svg {
  width: 17px;
  height: 17px;
}
.composer-heading small {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  color: #7e899c;
}
.composer-heading i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #aeb5c3;
}
.composer-heading .connected i {
  background: #40a780;
}
.assistant-composer textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 58px;
  margin: 10px 0;
  border: 0;
  resize: vertical;
  outline: 0;
  background: #fff;
  color: var(--ink);
  font:
    14px/1.9 'Microsoft YaHei',
    sans-serif;
}
.assistant-composer textarea::placeholder {
  color: #9aa3b5;
}
.assistant-composer:focus-within {
  border-color: #8d76c6;
  box-shadow: 0 0 0 3px #eee9f8;
}
.composer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #f0edf5;
  padding-top: 10px;
}
.composer-footer > span {
  color: #949bac;
  font-size: 10px;
}
.composer-footer b {
  margin: 0 5px;
  font-weight: 400;
}
.composer-footer button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 9px;
  color: #fff;
  background: var(--violet);
  cursor: pointer;
  font:
    600 12px 'Microsoft YaHei',
    sans-serif;
}
.composer-footer svg {
  width: 16px;
  height: 16px;
}
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  align-items: center;
  margin-top: 12px;
}
.suggestion-list > span {
  color: #949bad;
  font-size: 10px;
  margin-right: 3px;
}
.suggestion-list button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 9px;
  border: 1px solid #e4e6ef;
  border-radius: 7px;
  background: #fff9;
  color: #6d7690;
  font-size: 11px;
  text-align: left;
  cursor: pointer;
}
.suggestion-list svg {
  width: 11px;
  min-width: 11px;
}
.suggestion-list button:hover {
  color: #6750b5;
  border-color: #c4b6e0;
}
.assistant-notice,
.assistant-error {
  font-size: 11px;
  line-height: 1.8;
  margin: 12px 0 0;
  color: #7d8299;
}
.assistant-notice a,
.assistant-error a {
  color: #6750b5;
  margin-left: 7px;
}
.assistant-notice button {
  border: 0;
  padding: 0;
  margin-left: 7px;
  color: #6750b5;
  background: none;
  cursor: pointer;
}
.assistant-error {
  color: #b35353;
}
.assistant-results {
  grid-column: 1 / -1;
  padding-top: 25px;
  border-top: 1px solid #dde2ed;
  min-width: 0;
}
.result-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.result-heading > span,
.result-heading button {
  display: flex;
  gap: 7px;
  align-items: center;
}
.result-heading > span {
  font-size: 14px;
  font-weight: 700;
}
.result-heading svg {
  width: 16px;
  height: 16px;
}
.result-heading button {
  padding: 4px 0;
  border: 0;
  color: #7b7393;
  background: none;
  cursor: pointer;
  font-size: 11px;
}
.last-question {
  color: #898fa2;
  font-size: 12px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.result-message {
  white-space: pre-wrap;
  color: #47566b;
  font-size: 13px;
  line-height: 1.9;
}
.condition-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 15px 0;
}
.condition-list span {
  padding: 5px 9px;
  border-radius: 5px;
  color: #715e9a;
  background: #eae5f5;
  font-size: 11px;
}
.ai-hotel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 15px;
  margin-top: 20px;
}
.ai-hotel {
  min-width: 0;
  border: 1px solid #e1e5ef;
  border-radius: 11px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  background: #fff;
}
.ai-hotel > img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
}
.ai-hotel > div {
  padding: 15px;
}
.ai-hotel h3 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
  font-size: 14px;
}
.ai-hotel p {
  color: #8c94a5;
  font-size: 11px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ai-hotel > div > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.ai-hotel strong {
  color: #d16b4d;
  font-size: 18px;
}
.ai-hotel small {
  font-size: 10px;
  color: #9b9ca8;
  font-weight: 400;
  margin-left: 4px;
}
.ai-hotel b {
  font-size: 11px;
  font-weight: 500;
  color: #6750b5;
  display: flex;
  gap: 4px;
  align-items: center;
}
.ai-hotel b svg {
  width: 12px;
}
.result-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 20px;
  font-size: 10px;
  color: #929aac;
}
.result-footer a {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #6750b5;
  text-decoration: none;
  font-size: 12px;
}
.result-footer svg {
  width: 13px;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}
.loading-dot {
  width: 12px;
  height: 12px;
  border: 2px solid #ffffff50;
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid #8872c0;
  outline-offset: 4px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 760px) {
  .ai-stay {
    padding: 16px;
    gap: 12px;
    border-radius: 14px;
  }
  .assistant-intro {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }
  .assistant-intro h1 {
    font-size: 21px;
  }
  .manual-search {
    justify-self: start;
    font-size: 11px;
  }
  .assistant-composer {
    padding: 12px;
  }
  .composer-footer {
    align-items: flex-end;
  }
  .composer-footer > span {
    max-width: 100px;
    line-height: 1.7;
  }
  .composer-footer button {
    padding: 0 12px;
  }
  .suggestion-list > span {
    flex-basis: 100%;
  }
  .ai-hotel-grid {
    grid-template-columns: 1fr;
  }
  .ai-hotel {
    display: flex;
  }
  .ai-hotel > img {
    width: 100px;
    height: 115px;
    flex-shrink: 0;
  }
  .ai-hotel > div {
    flex: 1;
    min-width: 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  .loading-dot {
    animation: none;
  }
}
</style>
