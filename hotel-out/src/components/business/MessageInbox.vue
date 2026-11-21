<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChatDotRound, RefreshRight, Promotion } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useUserStore } from '@/stores'
import {
  getHotel,
  getHotelMessages,
  getMessageConversations,
  sendHotelMessage,
  getMerchantMessageConversations,
  getMerchantMessageThread,
  replyMerchantMessage,
} from '@/services/dataSource'

const props = defineProps({ merchant: Boolean })
const route = useRoute()
const router = useRouter()
const user = useUserStore()
const conversations = ref([])
const selected = ref('')
const messages = ref([])
const drafts = reactive({})
const loading = ref(true)
const refreshing = ref(false)
const sending = ref(false)
const error = ref('')
const sendError = ref('')
const mobileThread = ref(false)
const compactQuery = window.matchMedia(
  '(max-width:768px), (hover:none) and (pointer:coarse) and (max-width:1024px)',
)
const compact = ref(compactQuery.matches)
function onLayoutChange(event) {
  compact.value = event.matches
}
const timeline = ref(null)
let generation = 0
let timer
let alive = true
const key = (item) => (props.merchant ? `${item.hotelId}-${item.userId}` : String(item.hotelId))
const active = computed(() => conversations.value.find((item) => key(item) === selected.value))
const name = (item) => (props.merchant ? item.userName || '住客' : item.merchantName || '民宿商家')
const avatar = (item) => (props.merchant ? item.userAvatar || '' : item.merchantAvatar || '')
const draft = computed({
  get: () => drafts[selected.value] || '',
  set: (value) => {
    drafts[selected.value] = value
  },
})
const stamp = (value) => (value && dayjs(value).isValid() ? dayjs(value).format('MM-DD HH:mm') : '')

async function scrollLatest(force = false) {
  const element = timeline.value
  const nearBottom =
    !element || element.scrollHeight - element.scrollTop - element.clientHeight < 100
  await nextTick()
  if (timeline.value && (force || nearBottom))
    timeline.value.scrollTop = timeline.value.scrollHeight
}

async function refresh(initial = false) {
  if (sending.value || (!initial && refreshing.value)) return
  const current = ++generation
  refreshing.value = true
  if (initial) {
    loading.value = true
    messages.value = []
    sendError.value = ''
  }
  try {
    const rows = await (props.merchant
      ? getMerchantMessageConversations()
      : getMessageConversations())
    if (!alive || current !== generation) return
    let chosen = props.merchant ? selected.value : String(route.params.hotelId || '')
    // An enquiry can start before the first message has created a persisted conversation.
    if (!props.merchant && chosen && !rows.some((item) => key(item) === chosen)) {
      const hotel = await getHotel(chosen)
      if (!hotel?.merchant) throw new Error('该房源暂时无法联系商家，请返回房源页查看')
      rows.unshift({
        hotelId: hotel.id,
        hotelName: hotel.name,
        hotelImage: hotel.image,
        merchantName: hotel.merchant.companyName,
        merchantAvatar: hotel.merchant.avatar,
        unreadCount: 0,
      })
    }
    if (!alive || current !== generation) return
    if (props.merchant && !chosen && rows.length) chosen = key(rows[0])
    conversations.value = rows
    selected.value = chosen
    const item = rows.find((row) => key(row) === chosen)
    if (initial && item) mobileThread.value = true
    if (item && !item.unavailable && !document.hidden && (!compact.value || mobileThread.value)) {
      const thread = await (props.merchant
        ? getMerchantMessageThread(item.hotelId, item.userId)
        : getHotelMessages(item.hotelId))
      if (!alive || current !== generation) return
      const element = timeline.value
      const follow =
        !element || element.scrollHeight - element.scrollTop - element.clientHeight < 100
      messages.value = thread
      item.unreadCount = 0
      await scrollLatest(initial || follow)
    }
    error.value = ''
  } catch (failure) {
    if (alive && current === generation) error.value = failure.message || '消息暂时无法加载，请重试'
  } finally {
    if (alive && current === generation) {
      loading.value = false
      refreshing.value = false
    }
  }
}

async function open(item) {
  if (sending.value) return
  mobileThread.value = true
  if (props.merchant) {
    selected.value = key(item)
    await refresh(true)
  } else if (String(route.params.hotelId || '') !== String(item.hotelId)) {
    await router.push({ name: 'client-messages', params: { hotelId: item.hotelId } })
  } else await refresh(true)
}

async function send() {
  if (sending.value || loading.value || !active.value || active.value.unavailable) return
  const content = draft.value.trim()
  if (!content) return
  if (content.length > 1000) {
    sendError.value = '每条消息最多 1000 字'
    return
  }
  const item = active.value
  const conversationKey = key(item)
  ++generation
  refreshing.value = false
  sending.value = true
  sendError.value = ''
  try {
    const message = await (props.merchant
      ? replyMerchantMessage(item.hotelId, item.userId, content)
      : sendHotelMessage(item.hotelId, content))
    if (!alive) return
    drafts[conversationKey] = ''
    if (selected.value === conversationKey) {
      if (!messages.value.some((row) => row.id === message.id)) messages.value.push(message)
      item.latestMessage = content
      item.latestTime = message.createTime
      await scrollLatest(true)
    }
  } catch (failure) {
    if (alive) sendError.value = failure.message || '发送失败，请重试'
  } finally {
    if (alive) sending.value = false
  }
}
function shortcut(event) {
  if (event.key !== 'Enter' || (!event.ctrlKey && !event.metaKey)) return
  if (event.isComposing || event.keyCode === 229 || event.repeat) return
  event.preventDefault()
  send()
}
async function poll() {
  if (!alive) return
  if (!document.hidden) await refresh()
  if (alive) timer = window.setTimeout(poll, 5000)
}
function onVisible() {
  if (!document.hidden) refresh()
}
watch(
  () => route.params.hotelId,
  () => {
    selected.value = ''
    refresh(true)
  },
  { immediate: true },
)
onMounted(() => {
  compactQuery.addEventListener('change', onLayoutChange)
  timer = window.setTimeout(poll, 5000)
  document.addEventListener('visibilitychange', onVisible)
})
onBeforeUnmount(() => {
  compactQuery.removeEventListener('change', onLayoutChange)
  alive = false
  ++generation
  clearTimeout(timer)
  document.removeEventListener('visibilitychange', onVisible)
})
</script>

<template>
  <section class="chat-inbox" :class="{ 'show-thread': mobileThread }" aria-label="私信聊天">
    <aside class="chat-conversations" aria-label="会话列表">
      <header class="chat-list-heading">
        <div>
          <h2>{{ merchant ? '住客私信' : '我的私信' }}</h2>
          <span>{{ conversations.length }} 个会话</span>
        </div>
        <button
          class="chat-icon-button"
          type="button"
          aria-label="刷新会话"
          :disabled="refreshing || sending"
          @click="refresh()"
        >
          <RefreshRight />
        </button>
      </header>
      <div v-if="error && !mobileThread" class="chat-error" role="alert">
        {{ error }}<button type="button" @click="refresh(true)">重新加载</button>
      </div>
      <p v-if="loading && !conversations.length" class="chat-list-empty" role="status">
        正在加载会话…
      </p>
      <p v-else-if="!conversations.length && !error" class="chat-list-empty">
        {{
          merchant
            ? '暂无住客咨询，收到私信后会显示在这里'
            : '还没有咨询记录，进入民宿详情点击「私信商家」即可开始'
        }}
      </p>
      <button
        v-for="item in conversations"
        :key="key(item)"
        class="chat-conversation"
        :class="{ active: key(item) === selected }"
        :aria-current="key(item) === selected ? 'true' : undefined"
        :disabled="sending"
        @click="open(item)"
      >
        <UserAvatar :src="avatar(item)" :name="name(item)" />
        <span class="chat-conversation-copy"
          ><span
            ><strong>{{ name(item) }}</strong
            ><time>{{ stamp(item.latestTime) }}</time></span
          >
          <small>{{ item.hotelName }}</small
          ><span class="chat-preview">{{
            item.latestMessage || '咨询入住、房型与周边交通'
          }}</span> </span
        ><b
          v-if="item.unreadCount"
          class="chat-unread"
          :aria-label="`${item.unreadCount} 条未读`"
          >{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</b
        >
      </button>
      <RouterLink v-if="!merchant" to="/search" class="chat-find-stay">继续挑选住宿</RouterLink>
    </aside>

    <div class="chat-thread">
      <header class="chat-thread-heading">
        <button
          class="chat-icon-button chat-back"
          type="button"
          aria-label="返回会话列表"
          @click="mobileThread = false"
        >
          <ArrowLeft />
        </button>
        <template v-if="active"
          ><UserAvatar :src="avatar(active)" :name="name(active)" />
          <div>
            <h2>{{ name(active) }}</h2>
            <p>{{ active.hotelName }}</p>
          </div>
          <RouterLink
            :to="{ name: 'hotel-detail', params: { id: active.hotelId } }"
            class="chat-stay-link"
            >查看房源</RouterLink
          >
        </template>
        <h2 v-else>与{{ merchant ? '住客' : '商家' }}沟通</h2>
      </header>
      <div v-if="error && (mobileThread || active)" class="chat-error" role="alert">
        {{ error
        }}<button type="button" :disabled="sending" @click="refresh(true)">重新加载</button>
      </div>
      <template v-if="active">
        <div v-if="active.unavailable" class="chat-empty">
          <ChatDotRound />
          <h3>房源暂时无法咨询</h3>
          <p>该房源已下架或商家发生变更，请选择其他住宿。</p>
        </div>
        <template v-else>
          <div
            ref="timeline"
            class="chat-timeline"
            role="log"
            aria-label="聊天记录"
            aria-live="polite"
            :aria-busy="loading"
          >
            <div v-if="loading" class="chat-empty" role="status">正在加载聊天记录…</div>
            <div v-else-if="!messages.length && !error" class="chat-empty">
              <ChatDotRound />
              <h3>从一句问候开始</h3>
              <p>入住时间、停车位置、房型设施，都可以在这里咨询。</p>
            </div>
            <article
              v-for="message in messages"
              :key="message.id"
              class="chat-message"
              :class="{ mine: message.mine }"
            >
              <UserAvatar
                :src="message.mine ? user.userInfo?.avatar || '' : avatar(active)"
                :name="message.mine ? user.userInfo?.name || '我' : name(active)"
              />
              <div>
                <span class="chat-sender">{{ message.mine ? '我' : name(active) }}</span>
                <p>{{ message.content }}</p>
                <span class="chat-message-meta"
                  ><time>{{ stamp(message.createTime) }}</time
                  ><span v-if="message.mine">{{ message.read ? '已读' : '已发送' }}</span></span
                >
              </div>
            </article>
          </div>
          <form class="chat-composer" @submit.prevent="send">
            <label for="chat-draft">{{ merchant ? '回复住客' : '发送给商家' }}</label>
            <textarea
              id="chat-draft"
              v-model="draft"
              maxlength="1000"
              :disabled="sending"
              placeholder="输入消息，咨询入住相关问题…"
              @keydown="shortcut"
            />
            <p v-if="sendError" class="chat-send-error" role="alert">
              {{ sendError }}，内容已保留，可再次发送。
            </p>
            <div class="chat-composer-actions">
              <span
                ><span class="chat-key-hint">Ctrl / ⌘ + Enter 发送 · </span
                >{{ draft.length }}/1000</span
              ><button
                class="chat-send"
                type="submit"
                :disabled="sending || loading || !draft.trim()"
              >
                <Promotion aria-hidden="true" />{{ sending ? '发送中…' : '发送消息' }}
              </button>
            </div>
          </form>
        </template>
      </template>
      <div v-else class="chat-empty chat-welcome">
        <ChatDotRound />
        <h3>让入住前的沟通更轻松</h3>
        <p>选择一个会话，查看咨询和回复。</p>
        <RouterLink v-if="!merchant" to="/search">挑选民宿</RouterLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.chat-inbox {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  height: min(760px, calc(100dvh - 200px));
  min-height: 540px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface, #fff);
}
.chat-conversations {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow-y: auto;
  border-right: 1px solid var(--line);
}
.chat-list-heading,
.chat-thread-heading {
  display: flex;
  min-height: 84px;
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.chat-list-heading {
  justify-content: space-between;
}
.chat-list-heading h2,
.chat-thread-heading h2 {
  margin: 0;
  font-size: 17px;
  line-height: 1.5;
}
.chat-list-heading span,
.chat-thread-heading p {
  margin: 3px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.chat-icon-button {
  display: grid;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fff;
  color: var(--ink);
  cursor: pointer;
}
.chat-icon-button svg {
  width: 19px;
  height: 19px;
}
.chat-conversation {
  position: relative;
  display: flex;
  width: 100%;
  gap: 12px;
  padding: 20px 16px;
  border: 0;
  border-bottom: 1px solid var(--line);
  text-align: left;
  background: #fff;
  cursor: pointer;
  color: var(--ink);
}
.chat-conversation.active {
  box-shadow: inset 3px 0 var(--brand);
  background: var(--brand-soft);
}
.chat-conversation:hover {
  background: var(--brand-soft);
}
.chat-conversation-copy {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 5px;
}
.chat-conversation-copy > span:first-child {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.chat-conversation-copy strong {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-conversation-copy time {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--muted);
}
.chat-conversation-copy small,
.chat-preview {
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-unread {
  position: absolute;
  right: 12px;
  bottom: 13px;
  min-width: 18px;
  padding: 2px 5px;
  border-radius: 12px;
  background: var(--brand);
  color: #fff;
  font-size: 10px;
}
.chat-find-stay {
  margin-top: auto;
  padding: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--brand);
}
.chat-list-empty {
  padding: 24px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}
.chat-thread {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
}
.chat-thread-heading > div {
  min-width: 0;
}
.chat-thread-heading p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-stay-link {
  margin-left: auto;
  flex-shrink: 0;
  color: var(--brand);
  font-size: 13px;
}
.chat-timeline {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 24px;
  background: var(--page, #f5f6f8);
}
.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 22px;
}
.chat-message > div {
  min-width: 0;
  max-width: 76%;
}
.chat-message.mine {
  flex-direction: row-reverse;
}
.chat-message p {
  margin: 6px 0;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: 2px 10px 10px;
  background: #fff;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 14px;
  line-height: 1.7;
}
.chat-message.mine p {
  border-color: var(--brand);
  border-radius: 10px 2px 10px 10px;
  background: var(--brand);
  color: #fff;
}
.chat-sender,
.chat-message-meta {
  display: flex;
  gap: 8px;
  color: var(--muted);
  font-size: 11px;
}
.mine .chat-sender,
.mine .chat-message-meta {
  justify-content: flex-end;
}
.chat-empty {
  display: flex;
  min-height: 220px;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px;
  text-align: center;
  color: var(--muted);
}
.chat-empty > svg {
  width: 40px;
  height: 40px;
  color: var(--brand);
}
.chat-empty h3 {
  margin: 0;
  color: var(--ink);
  font-size: 18px;
}
.chat-empty p {
  max-width: 300px;
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
}
.chat-welcome {
  flex: 1;
}
.chat-empty a {
  color: var(--brand);
}
.chat-composer {
  flex-shrink: 0;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid var(--line);
}
.chat-composer label {
  display: block;
  margin-bottom: 8px;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}
.chat-composer textarea {
  width: 100%;
  min-height: 72px;
  max-height: 160px;
  resize: vertical;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  font: inherit;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink);
  background: #fff;
}
.chat-composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  color: var(--muted);
  font-size: 11px;
}
.chat-send {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border: 0;
  border-radius: 6px;
  background: var(--brand);
  color: #fff;
  font: inherit;
  font-size: 14px;
  cursor: pointer;
}
.chat-send svg {
  width: 17px;
  height: 17px;
}
.chat-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  background: #fff3f1;
  color: #b42318;
  font-size: 13px;
}
.chat-error button {
  min-height: 44px;
  flex-shrink: 0;
  border: 0;
  background: transparent;
  color: #b42318;
  text-decoration: underline;
  cursor: pointer;
}
.chat-send-error {
  margin: 8px 0;
  color: #b42318;
  font-size: 12px;
}
.chat-back {
  display: none;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
button:focus-visible,
textarea:focus-visible,
a:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 3px;
}
@media (max-width: 1000px) {
  .chat-inbox {
    grid-template-columns: 260px minmax(0, 1fr);
  }
  .chat-list-heading,
  .chat-thread-heading {
    padding: 12px;
  }
  .chat-conversation {
    padding: 16px 12px;
  }
  .chat-conversation-copy > span:first-child {
    flex-wrap: wrap;
    gap: 3px;
  }
  .chat-timeline {
    padding: 16px;
  }
}
@media (max-width: 768px), (hover: none) and (pointer: coarse) and (max-width: 1024px) {
  .chat-inbox {
    display: block;
    height: calc(100dvh - 190px - env(safe-area-inset-bottom, 0px));
    min-height: 440px;
  }
  .chat-conversations {
    height: 100%;
    border: 0;
  }
  .chat-thread {
    display: none;
    height: 100%;
  }
  .show-thread .chat-conversations {
    display: none;
  }
  .show-thread .chat-thread {
    display: flex;
  }
  .chat-back {
    display: grid;
  }
  .chat-thread-heading {
    min-height: 72px;
    gap: 8px;
  }
  .chat-thread-heading h2 {
    font-size: 15px;
  }
  .chat-thread-heading > .account-avatar {
    width: 32px;
    height: 32px;
  }
  .chat-thread-heading > div {
    flex: 1;
  }
  .chat-stay-link {
    font-size: 12px;
  }
  .chat-composer {
    padding: 12px;
  }
  .chat-composer textarea {
    font-size: 16px;
    min-height: 64px;
    max-height: 120px;
  }
  .chat-key-hint {
    display: none;
  }
  .chat-message > div {
    max-width: 78%;
  }
  .chat-timeline {
    padding: 16px 12px;
  }
  .chat-conversation-copy > span:first-child {
    flex-wrap: nowrap;
  }
}
@media (max-height: 550px) {
  .chat-inbox {
    min-height: 440px;
    height: calc(100dvh - 100px);
  }
}
</style>
