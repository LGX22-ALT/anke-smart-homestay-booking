<script setup>
import dayjs from 'dayjs'
import { computed, ref, watch } from 'vue'

const props = defineProps({
  rows: { type: Array, default: () => [] },
})

const emit = defineEmits(['edit', 'month-change'])
const monthOffset = ref(0)
const month = computed(() => dayjs().add(monthOffset.value, 'month').startOf('month'))
watch(month, (date) => emit('month-change', date.format('YYYY-MM-DD')))
const days = computed(() => {
  const firstWeekday = month.value.day()
  const count = month.value.daysInMonth()
  return [
    ...Array.from({ length: firstWeekday }, () => null),
    ...Array.from({ length: count }, (_, index) => {
      const date = month.value.date(index + 1)
      const row = props.rows.find((item) => item.calendarDate === date.format('YYYY-MM-DD'))
      return {
        date: date.format('YYYY-MM-DD'),
        day: index + 1,
        price: row ? Number(row.price) : null,
        stock: row ? Number(row.availableStock) : 0,
        status: !row
          ? '未设置'
          : Number(row.isAvailable) !== 1
            ? '关闭'
            : Number(row.availableStock) < 1
              ? '售罄'
              : '可售',
      }
    }),
  ]
})
</script>

<template>
  <div class="room-calendar">
    <div class="calendar-head">
      <button type="button" aria-label="上个月" @click="monthOffset -= 1">←</button>
      <strong>{{ month.format('YYYY 年 MM 月') }}</strong>
      <button type="button" aria-label="下个月" @click="monthOffset += 1">→</button>
    </div>
    <div class="week-row">
      <span v-for="weekday in ['日', '一', '二', '三', '四', '五', '六']" :key="weekday"
        >周{{ weekday }}</span
      >
    </div>
    <div class="day-grid">
      <span
        v-for="(day, index) in days"
        :key="day?.date || `empty-${index}`"
        :class="['day-cell', day && `day-cell--${day.status}`]"
      >
        <button v-if="day" type="button" @click="emit('edit', day)">
          <strong>{{ day.day }}</strong>
          <small>{{ day.status }}</small>
          <em>{{ day.price === null ? '—' : `¥${day.price}` }}</em>
          <span>余 {{ day.stock }}</span>
        </button>
      </span>
    </div>
  </div>
</template>

<style scoped>
.room-calendar {
  border: 1px solid var(--line);
  background: #fff;
}
.calendar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px;
  border-bottom: 1px solid #e4e7eb;
}
.calendar-head button {
  width: 34px;
  height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
}
.week-row,
.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.week-row span {
  padding: 10px;
  color: var(--muted);
  font-size: 11px;
  text-align: center;
}
.day-cell {
  min-height: 105px;
  border-top: 1px solid #e4e7eb;
  border-right: 1px solid #e4e7eb;
}
.day-cell button {
  display: grid;
  width: 100%;
  height: 100%;
  min-height: 104px;
  padding: 9px;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.day-cell strong {
  color: #243b53;
}
.day-cell small {
  color: var(--brand);
}
.day-cell em {
  color: #52606d;
  font-size: 12px;
  font-style: normal;
}
.day-cell span {
  color: var(--muted);
  font-size: 11px;
}
.day-cell--售罄 {
  background: #fef3f2;
}
.day-cell--售罄 small {
  color: #d64545;
}
.day-cell--关闭 {
  background: #f0f4f8;
}
.day-cell--关闭 small {
  color: var(--muted);
}
@media (max-width: 700px) {
  .day-cell {
    min-height: 76px;
  }
  .day-cell button {
    min-height: 75px;
    padding: 5px;
  }
  .day-cell em,
  .day-cell span {
    display: none;
  }
}
</style>
