import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getStayNights } from '@/utils/date'
import { createRequestId } from '@/utils/requestId'

export const useBookingStore = defineStore('booking', () => {
  const hotel = ref(null)
  const room = ref(null)
  const checkInDate = ref('')
  const checkOutDate = ref('')
  const guestCount = ref(1)
  const requestId = ref('')

  const stayNights = computed(() => getStayNights(checkInDate.value, checkOutDate.value))

  function setBooking(booking) {
    requestId.value = createRequestId()
    hotel.value = booking.hotel ?? null
    room.value = booking.room ?? null
    checkInDate.value = booking.checkInDate ?? ''
    checkOutDate.value = booking.checkOutDate ?? ''
    guestCount.value =
      Number.isInteger(booking.guestCount) && booking.guestCount > 0 ? booking.guestCount : 1
  }

  function clearBooking() {
    hotel.value = null
    room.value = null
    checkInDate.value = ''
    checkOutDate.value = ''
    guestCount.value = 1
    requestId.value = ''
  }

  return {
    hotel,
    room,
    checkInDate,
    checkOutDate,
    guestCount,
    requestId,
    stayNights,
    setBooking,
    clearBooking,
  }
})
