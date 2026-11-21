import { ref } from 'vue'
const HISTORY_KEY = 'hotel_browse_history'
const COMPARE_KEY = 'hotel_compare'
function read(key) {
  try {
    const value = JSON.parse(localStorage.getItem(key) || '[]')
    return Array.isArray(value)
      ? value
          .filter(
            (item) =>
              item &&
              (typeof item.id === 'string' || typeof item.id === 'number') &&
              typeof item.name === 'string',
          )
          .slice(0, 20)
      : []
  } catch {
    return []
  }
}
function write(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    /* Browsing remains available when storage is full or disabled. */
  }
}
export const browseHistory = ref(read(HISTORY_KEY))
export const compareHotels = ref(read(COMPARE_KEY).slice(0, 3))
function summary(hotel) {
  return {
    id: hotel.id,
    name: hotel.name,
    image: hotel.image,
    city: hotel.city,
    district: hotel.district,
    price: hotel.price,
    score: hotel.score,
    type: hotel.type,
  }
}
export function recordHotel(hotel) {
  if (!hotel?.id) return
  browseHistory.value = [
    { ...summary(hotel), visitedAt: new Date().toISOString() },
    ...browseHistory.value.filter((item) => String(item.id) !== String(hotel.id)),
  ].slice(0, 20)
  write(HISTORY_KEY, browseHistory.value)
}
export function clearHistory() {
  browseHistory.value = []
  write(HISTORY_KEY, [])
}
export function toggleCompare(hotel) {
  const exists = compareHotels.value.some((item) => String(item.id) === String(hotel.id))
  if (!exists && compareHotels.value.length >= 3) return false
  compareHotels.value = exists
    ? compareHotels.value.filter((item) => String(item.id) !== String(hotel.id))
    : [...compareHotels.value, summary(hotel)]
  write(COMPARE_KEY, compareHotels.value)
  return true
}
