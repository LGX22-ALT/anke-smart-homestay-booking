<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import RoomStatusCalendar from '@/components/business/RoomCalendar.vue'
import {
  getMerchantHotels,
  getMerchantRoomTypes,
  getMerchantCalendar,
  saveMerchantCalendar,
} from '@/services/dataSource'

const hotels = ref([])
const rooms = ref([])
const calendar = ref([])
const selectedHotel = ref(null)
const selectedRoom = ref(null)
const month = ref(dayjs().startOf('month').format('YYYY-MM-DD'))
const editing = ref(false)
const saving = ref(false)
const form = reactive({ date: '', price: 0, stock: 0, status: '可售' })
const range = computed(() => ({
  startDate: month.value,
  endDate: dayjs(month.value).endOf('month').format('YYYY-MM-DD'),
}))
const { loading, error, load } = useLoadState(
  async () => {
    const result = await getMerchantHotels()
    hotels.value = result
    if (!result.some((hotel) => hotel.id === selectedHotel.value))
      selectedHotel.value = result[0]?.id || null
    await loadRooms()
  },
  () => {
    hotels.value = []
    rooms.value = []
    calendar.value = []
  },
)
const {
  loading: calendarLoading,
  error: calendarError,
  load: loadCalendar,
} = useLoadState(
  async (isCurrent) => {
    if (!selectedRoom.value) return
    const result = await getMerchantCalendar({ roomTypeId: selectedRoom.value, ...range.value })
    if (isCurrent()) calendar.value = result
  },
  () => {
    calendar.value = []
  },
)
async function loadRooms() {
  rooms.value = []
  selectedRoom.value = null
  calendar.value = []
  if (!selectedHotel.value) return
  rooms.value = await getMerchantRoomTypes(selectedHotel.value)
  selectedRoom.value = rooms.value[0]?.id || null
  await loadCalendar()
}
async function changeHotel() {
  loading.value = true
  error.value = ''
  try {
    await loadRooms()
  } catch (cause) {
    error.value = cause.message || '房型加载失败'
  } finally {
    loading.value = false
  }
}
function changeMonth(value) {
  month.value = value
  loadCalendar()
}
function editDay(day) {
  if (
    loading.value ||
    calendarLoading.value ||
    error.value ||
    calendarError.value ||
    !selectedRoom.value
  )
    return
  Object.assign(form, day, {
    price: day.price ?? 0,
    status: day.status === '未设置' ? '可售' : day.status,
  })
  editing.value = true
}
async function save() {
  if (saving.value) return
  saving.value = true
  try {
    await saveMerchantCalendar({
      roomTypeId: selectedRoom.value,
      startDate: form.date,
      endDate: form.date,
      price: form.price,
      stock: form.status === '售罄' ? 0 : form.stock,
      available: form.status !== '关闭',
    })
    editing.value = false
    await loadCalendar()
    ElMessage.success('房态已保存')
  } catch {
    /* request interceptor displays the API error */
  } finally {
    saving.value = false
  }
}
onMounted(load)
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <h1>房态日历</h1>
        <p>按日维护房价、库存和销售状态。</p>
      </div>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <LoadState
      v-if="!error"
      :loading="calendarLoading"
      :error="calendarError"
      @retry="loadCalendar"
    />
    <p v-if="!loading && !error && !hotels.length" class="bo-empty">暂无房源，请先添加房源。</p>
    <section class="bo-panel selector-row">
      <label class="bo-field"
        >选择房源<select v-model="selectedHotel" :disabled="loading" @change="changeHotel">
          <option v-for="hotel in hotels" :key="hotel.id" :value="hotel.id">
            {{ hotel.name }}
          </option>
        </select></label
      ><label class="bo-field"
        >选择房型<select
          v-model="selectedRoom"
          :disabled="loading || calendarLoading"
          @change="loadCalendar"
        >
          <option v-for="room in rooms" :key="room.id" :value="room.id">
            {{ room.name }}
          </option>
        </select></label
      >
      <div class="legend">
        <span><i class="available"></i>可售</span><span><i class="sold"></i>售罄</span
        ><span><i class="closed"></i>关闭</span>
      </div>
    </section>
    <RoomStatusCalendar
      v-if="!error && selectedRoom"
      :rows="calendar"
      @month-change="changeMonth"
      @edit="editDay"
    />
    <el-dialog v-model="editing" title="调整单日房态" width="min(480px, 92vw)"
      ><div class="bo-form-grid">
        <label class="bo-field">日期<input v-model="form.date" disabled /></label
        ><label class="bo-field"
          >状态<select v-model="form.status">
            <option>可售</option>
            <option>售罄</option>
            <option>关闭</option>
          </select></label
        ><label class="bo-field"
          >当日价格<input v-model.number="form.price" type="number" min="0" /></label
        ><label class="bo-field"
          >可售库存<input v-model.number="form.stock" type="number" min="0"
        /></label>
      </div>
      <template #footer
        ><button class="bo-button" type="button" @click="editing = false">取消</button
        ><button
          class="bo-button bo-button--primary"
          type="button"
          :disabled="saving"
          @click="save"
        >
          保存调整
        </button></template
      ></el-dialog
    >
  </div>
</template>

<style scoped>
.selector-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  align-items: end;
  gap: 16px;
}
.legend {
  display: flex;
  gap: 12px;
  padding-bottom: 11px;
  color: var(--muted);
  font-size: 11px;
}
.legend span {
  display: flex;
  align-items: center;
  gap: 5px;
}
.legend i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2f9e83;
}
.legend .sold {
  background: #d64545;
}
.legend .closed {
  background: var(--muted);
}
@media (max-width: 760px) {
  .selector-row {
    grid-template-columns: 1fr;
  }
  .legend {
    padding: 0;
  }
}
</style>
