import { banners, hotels, initialOrders, reviews, users } from './data'

const ORDERS_KEY = 'hotel_mock_orders'
const FAVORITES_KEY = 'hotel_mock_favorites'

function readJson(key, fallback) {
  try {
    const value = localStorage.getItem(key)
    return value ? JSON.parse(value) : fallback
  } catch {
    return fallback
  }
}

function writeJson(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
  return value
}

export function listHotels(filters = {}) {
  const keyword = String(filters.keyword || '')
    .trim()
    .toLowerCase()
  const maxPrice = Number(filters.maxPrice || 0)
  let result = hotels.filter((hotel) => hotel.status === '已上架')

  if (keyword) {
    result = result.filter((hotel) =>
      [hotel.name, hotel.city, hotel.district, hotel.address, ...hotel.tags]
        .join(' ')
        .toLowerCase()
        .includes(keyword),
    )
  }

  if (filters.type) result = result.filter((hotel) => hotel.type === filters.type)
  if (filters.city) result = result.filter((hotel) => hotel.city === filters.city)
  if (filters.district) result = result.filter((hotel) => hotel.district === filters.district)
  if (maxPrice) result = result.filter((hotel) => hotel.price <= maxPrice)
  if (filters.facility)
    result = result.filter((hotel) => hotel.facilities.includes(filters.facility))
  if (filters.bed)
    result = result.filter((hotel) => hotel.rooms.some((room) => room.bed.includes(filters.bed)))
  if (Number(filters.guests || 0))
    result = result.filter((hotel) =>
      hotel.rooms.some((room) => room.guests >= Number(filters.guests)),
    )

  return [...result].sort((a, b) => {
    if (filters.sort === 'priceAsc') return a.price - b.price
    if (filters.sort === 'priceDesc') return b.price - a.price
    if (filters.sort === 'sales') return (b.sales || b.id * 17) - (a.sales || a.id * 17)
    if (filters.sort === 'reviews') return (b.reviews?.length || 0) - (a.reviews?.length || 0)
    return b.score - a.score
  })
}

export function getHotelById(id) {
  return hotels.find((hotel) => hotel.id === Number(id)) || null
}

export function listOrders() {
  return readJson(ORDERS_KEY, initialOrders)
}

export function createOrder(payload) {
  const order = {
    ...payload,
    id: `HO${Date.now()}`,
    status: '待支付',
    createdAt: new Date().toLocaleString('zh-CN', { hour12: false }),
  }
  writeJson(ORDERS_KEY, [order, ...listOrders()])
  return order
}

export function updateOrderStatus(id, status) {
  const orders = listOrders().map((order) => (order.id === id ? { ...order, status } : order))
  writeJson(ORDERS_KEY, orders)
  return orders.find((order) => order.id === id)
}

export function getFavorites() {
  return readJson(FAVORITES_KEY, [1, 2])
}

export function toggleFavorite(hotelId) {
  const favorites = getFavorites()
  const next = favorites.includes(hotelId)
    ? favorites.filter((id) => id !== hotelId)
    : [...favorites, hotelId]
  return writeJson(FAVORITES_KEY, next)
}

export function getAdminData() {
  return { hotels, orders: listOrders(), users, reviews, banners }
}

export { banners, hotels, reviews, users }
