import * as adminApi from '@/api/modules/admin'
import * as hotelApi from '@/api/modules/hotel'
import * as merchantApi from '@/api/modules/merchant'
import * as messageApi from '@/api/modules/message'
import * as orderApi from '@/api/modules/order'
import * as userApi from '@/api/modules/user'
import { clearAuthSession, getRefreshToken, getUserInfo } from '@/utils/auth'
import { toOrderDisplay } from '@/utils/orderDisplay'
export const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const mock = useMock ? import('../mocks/service') : null

const orderStatusText = {
  PENDING_PAYMENT: '待支付',
  PAID: '待确认',
  PENDING_CHECKIN: '待入住',
  CONFIRMED: '待入住',
  CHECKED_IN: '已入住',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  PLATFORM_INTERVENED: '平台介入',
}
const hotelStatusText = {
  ONLINE: '已上架',
  OFFLINE: '已下架',
  PENDING_AUDIT: '待审核',
  REJECTED: '已驳回',
}

function parseJson(value, fallback = []) {
  if (Array.isArray(value)) return value
  try {
    const parsed = value ? JSON.parse(value) : fallback
    return Array.isArray(parsed) ? parsed : fallback
  } catch {
    return fallback
  }
}

function pageList(result) {
  return Array.isArray(result) ? result : result?.list || []
}

export function normalizeHotel(
  hotel,
  rooms = hotel?.rooms,
  reviews = hotel?.reviews,
  merchant = hotel?.merchant,
) {
  if (!hotel) return null
  return {
    ...hotel,
    image: hotel.image || hotel.coverImage,
    gallery: parseJson(hotel.gallery, [hotel.coverImage].filter(Boolean)),
    facilities: parseJson(hotel.facilities, []),
    nearby: parseJson(hotel.nearbyInfo, Array.isArray(hotel.nearby) ? hotel.nearby : []),
    merchant: merchant || hotel.merchant || null,
    rooms: (rooms || []).map((room) => ({
      ...room,
      price: room.price ?? room.basePrice ?? 0,
      guests: room.guests ?? room.maxGuests ?? 1,
      facilities: parseJson(room.facilities, []),
    })),
    reviews: reviews || [],
    price: hotel.price ?? hotel.minPrice ?? 0,
    score: hotel.score ?? 0,
    status: hotelStatusText[hotel.status] || hotel.status,
  }
}

export function normalizeOrder(order, hotel = null) {
  const display = toOrderDisplay(order, hotel)
  return {
    ...display,
    hotel: display.hotelName,
    guest: display.guestName,
    phone: display.guestPhone,
    orderStatus:
      display.orderStatus ||
      Object.keys(orderStatusText).find((key) => orderStatusText[key] === order.status),
    status: display.statusText,
  }
}

function normalizeReview(review) {
  return {
    ...review,
    hotel: review.hotel || `房源 #${review.hotelId || '-'}`,
    user: review.user || `用户 #${review.userId || '-'}`,
    date: review.date || review.createTime || '',
    reply: review.reply || review.merchantReply || '',
    status:
      review.status === 'VISIBLE' ? '正常' : review.status === 'HIDDEN' ? '已隐藏' : review.status,
  }
}

export async function listHotels(params = {}) {
  if (useMock) return (await mock).listHotels(params)
  const result = await hotelApi.getHotelList(params)
  return pageList(result).map((hotel) => normalizeHotel(hotel))
}

export async function getHotelPage(params = {}) {
  if (useMock) {
    const rows = (await mock).listHotels(params)
    const size = Number(params.pageSize || 10)
    const page = Number(params.pageNum || 1)
    return {
      total: rows.length,
      pages: Math.ceil(rows.length / size),
      list: rows.slice((page - 1) * size, page * size),
    }
  }
  const result = await hotelApi.getHotelList(params)
  return {
    total: Number(result.total || 0),
    pages: Number(result.pages || 0),
    list: pageList(result).map((hotel) => normalizeHotel(hotel)),
  }
}

export async function getHotel(id) {
  if (useMock) return (await mock).getHotelById(id)
  const result = await hotelApi.getHotelDetail(id)
  return normalizeHotel(result?.hotel || result, result?.rooms, result?.reviews, result?.merchant)
}

export async function getHotelMessages(hotelId) {
  if (useMock) return []
  return messageApi.getHotelMessages(hotelId)
}

export async function getMessageConversations() {
  return useMock ? [] : messageApi.getMessageConversations()
}

export async function sendHotelMessage(hotelId, content) {
  if (useMock)
    return {
      id: Date.now(),
      content,
      senderRole: 'USER',
      mine: true,
      read: false,
      createTime: new Date().toISOString(),
    }
  return messageApi.sendHotelMessage(hotelId, { content })
}

export async function getAvailability(id, params) {
  if (useMock) return []
  return hotelApi.checkRoomAvailability(id, params)
}

export async function login(data) {
  if (useMock) return null
  return userApi.login(data)
}

export async function register(data) {
  if (useMock) return null
  return userApi.register(data)
}

export async function logoutSession() {
  const refreshToken = getRefreshToken()
  try {
    if (!useMock && refreshToken) await userApi.logout({ refreshToken })
  } catch {
    // Local sign-out must still complete when the server is unavailable.
  } finally {
    clearAuthSession()
  }
}

export async function getProfile() {
  if (useMock) return getUserInfo()
  return userApi.getProfile()
}

export async function saveProfile(data) {
  if (useMock) return { ...getUserInfo(), ...data, username: data.name || getUserInfo()?.name }
  return userApi.updateProfile(data)
}

export async function uploadAvatar(file) {
  if (useMock) throw new Error('演示模式暂不支持上传头像，请选择默认头像')
  return userApi.uploadAvatar(file)
}

export async function getOrders(params = {}) {
  if (useMock) return (await mock).listOrders()
  const result = await orderApi.getOrderList(params)
  return pageList(result).map(normalizeOrder)
}

export async function createBooking(data) {
  if (useMock) {
    const hotel = (await mock).getHotelById(data.hotelId)
    const room = hotel?.rooms.find((room) => room.id === data.roomTypeId)
    if (!room) throw new Error('房型不存在')
    const nights = Math.round((new Date(data.checkOut) - new Date(data.checkIn)) / 86400000)
    return normalizeOrder((await mock).createOrder({ ...data, amount: room.price * nights }))
  }
  return normalizeOrder(await orderApi.createOrder(data))
}

export async function payBooking(id, success = true) {
  if (useMock) return (await mock).updateOrderStatus(id, success ? '待入住' : '待支付')
  return normalizeOrder(await orderApi.mockPayOrder(id, success))
}

export async function cancelBooking(id, reason) {
  if (useMock) return (await mock).updateOrderStatus(id, '已取消')
  return normalizeOrder(await orderApi.cancelOrder(id, { reason }))
}

export async function refundBooking(id) {
  if (useMock) return (await mock).updateOrderStatus(id, '退款中')
  return normalizeOrder(await orderApi.refundOrder(id))
}

export async function getUserFavorites() {
  return useMock ? (await mock).getFavorites() : userApi.getFavorites()
}

export async function toggleUserFavorite(id) {
  return useMock ? (await mock).toggleFavorite(id) : userApi.toggleFavorite(id)
}

export async function getUserFavoriteHotels() {
  const ids = await getUserFavorites()
  if (useMock) return (await mock).hotels.filter((hotel) => ids.includes(hotel.id))
  return Promise.all(ids.map(getHotel))
}

export async function setUserFavorite(id, enabled) {
  if (useMock) {
    const current = (await mock).getFavorites()
    return current.includes(id) === enabled ? current : (await mock).toggleFavorite(id)
  }
  return enabled ? userApi.addFavorite(id) : userApi.removeFavorite(id)
}

export async function getUserComments() {
  if (useMock) return (await mock).reviews.slice(0, 2)
  return (await userApi.getComments()).map(normalizeReview)
}

export async function updateUserComment(id, data) {
  if (useMock) return { id, ...data }
  return normalizeReview(await userApi.updateComment(id, data))
}

export async function deleteUserComment(id) {
  if (!useMock) await userApi.deleteComment(id)
}

export async function getMerchantOverview() {
  return useMock ? (await mock).getAdminData() : merchantApi.getMerchantDashboard()
}

export async function getMerchantHotels(params = {}) {
  if (useMock) return (await mock).hotels.map((hotel) => ({ ...hotel }))
  return (await allPages(merchantApi.getMerchantHotels, params)).map((hotel) =>
    normalizeHotel(hotel),
  )
}

export async function saveMerchantHotel(data, id) {
  const nearby = Array.isArray(data.nearby)
    ? data.nearby
    : String(data.nearbyText || '')
        .split(/\r?\n/)
        .map((line) => line.split(/[|｜]/).map((item) => item.trim()))
        .filter((parts) => parts[1])
        .map(([type, name, distance, description]) => ({ type, name, distance, description }))
  const payload = {
    ...data,
    coverImage: data.coverImage || data.image || data.images?.[0] || '',
    gallery: JSON.stringify(data.gallery || data.images || []),
    facilities: JSON.stringify(
      Array.isArray(data.facilities) ? data.facilities : String(data.facilities || '').split('、'),
    ),
    minPrice: data.minPrice ?? data.price ?? 0,
    district: data.district || '',
    nearbyInfo: JSON.stringify(nearby),
  }
  if (useMock) return normalizeHotel({ ...data, id: id || Date.now() })
  return normalizeHotel(
    id
      ? await merchantApi.updateMerchantHotel(id, payload)
      : await merchantApi.saveMerchantHotel(payload),
  )
}

export async function setMerchantHotelStatus(id, status) {
  if (useMock) return null
  const raw = { 已上架: 'ONLINE', 已下架: 'OFFLINE', 待审核: 'PENDING_AUDIT' }[status] || status
  return normalizeHotel(await merchantApi.updateMerchantHotelStatus(id, raw))
}

export async function getMerchantCalendar(params) {
  return useMock ? [] : merchantApi.getRoomCalendar(params)
}

export async function getMerchantRoomTypes(id) {
  if (useMock) return (await mock).getHotelById(id)?.rooms || []
  return merchantApi.getMerchantRoomTypes(id)
}

export async function saveMerchantCalendar(data) {
  return useMock ? data : merchantApi.updateRoomCalendar(data)
}

export async function getMerchantOrders(params = {}) {
  if (useMock) return (await mock).listOrders()
  return (await allPages(merchantApi.getMerchantOrders, params)).map(normalizeOrder)
}

export async function transitionMerchantOrder(id, action) {
  if (useMock) {
    const status = { confirm: '待入住', checkIn: '已入住', cancel: '已取消' }[action]
    return (await mock).updateOrderStatus(id, status)
  }
  const methods = {
    confirm: merchantApi.confirmMerchantOrder,
    checkIn: merchantApi.checkInMerchantOrder,
    cancel: merchantApi.cancelMerchantOrder,
  }
  return normalizeOrder(await methods[action](id))
}

export async function getMerchantReviews() {
  if (useMock) return (await mock).reviews.map((review) => ({ ...review }))
  return (await merchantApi.getMerchantReviews()).map(normalizeReview)
}

export async function replyMerchantReview(id, reply) {
  if (useMock) return { id, reply }
  return normalizeReview(await merchantApi.replyReview(id, { reply }))
}

export async function getMerchantStatistics() {
  if (useMock) return (await mock).getAdminData()
  return merchantApi.getMerchantStatistics()
}

export async function getMerchantMessageConversations() {
  return useMock ? [] : merchantApi.getMessageConversations()
}

export async function getMerchantMessageThread(hotelId, userId) {
  return useMock ? [] : merchantApi.getMessageThread(hotelId, userId)
}

export async function replyMerchantMessage(hotelId, userId, content) {
  if (useMock)
    return {
      id: Date.now(),
      content,
      senderRole: 'MERCHANT',
      mine: true,
      read: false,
      createTime: new Date().toISOString(),
    }
  return merchantApi.replyMessage(hotelId, userId, { content })
}

export async function getAdminOverview() {
  return useMock ? (await mock).getAdminData() : adminApi.getAdminDashboard()
}

export async function getAdminUsers(params = {}) {
  if (useMock) return (await mock).users.map((user) => ({ ...user }))
  return (await allPages(adminApi.getAdminUsers, params)).map((user) => ({
    ...user,
    registeredAt: user.registeredAt || user.createTime,
    status: user.status === 1 ? '正常' : '已禁用',
  }))
}

export async function setAdminUserStatus(id, enabled) {
  if (useMock) return null
  return adminApi.updateUserStatus(id, enabled ? 1 : 0)
}

export async function getAdminHotelAudits(params = {}) {
  if (useMock) return (await mock).hotels.map((hotel) => ({ ...hotel }))
  return (await adminApi.getHotelAudits(params)).map((hotel) => normalizeHotel(hotel))
}

export async function auditAdminHotel(id, approved, reason = '') {
  if (useMock) return null
  return adminApi.auditHotel(id, { result: approved ? 'APPROVED' : 'REJECTED', reason })
}

export async function getAdminOrders(params = {}) {
  if (useMock) return (await mock).listOrders()
  return (await allPages(adminApi.getPlatformOrders, params)).map(normalizeOrder)
}

export async function interveneAdminOrder(id, data) {
  if (useMock) return (await mock).updateOrderStatus(id, orderStatusText[data.status])
  return normalizeOrder(await adminApi.interveneOrder(id, data))
}

export async function getAdminReviews() {
  if (useMock) return (await mock).reviews.map((review) => ({ ...review }))
  return (await adminApi.getAdminReviews()).map(normalizeReview)
}

export async function moderateAdminReview(id, visible) {
  if (useMock) return null
  return adminApi.moderateReview(id, { status: visible ? 'VISIBLE' : 'HIDDEN' })
}

export async function getAdminBanners() {
  return useMock ? (await mock).banners.map((banner) => ({ ...banner })) : adminApi.getBanners()
}

export async function saveAdminBanner(data, id) {
  if (useMock) return { ...data, id: id || Date.now() }
  return id ? adminApi.updateBanner(id, data) : adminApi.createBanner(data)
}

export async function getAdminDicts(params = {}) {
  return useMock ? [] : adminApi.getSystemDicts(params)
}

export async function saveAdminDict(data, id) {
  if (useMock) return { ...data, id: id || Date.now() }
  return id ? adminApi.updateSystemDict(id, data) : adminApi.createSystemDict(data)
}

async function allPages(fetcher, params = {}) {
  const first = await fetcher({ ...params, pageNum: 1, pageSize: 100 })
  const list = [...pageList(first)]
  for (let pageNum = 2; pageNum <= Number(first.pages || 1); pageNum++) {
    list.push(...pageList(await fetcher({ ...params, pageNum, pageSize: 100 })))
  }
  return list
}

function mockOrderPage(rows, params, getHotelById) {
  const filtered = rows.filter(
    (row) =>
      (!params.status ||
        (row.orderStatus ||
          Object.keys(orderStatusText).find((key) => orderStatusText[key] === row.status)) ===
          params.status) &&
      (!params.keyword || String(row.orderNo || row.id).includes(params.keyword)),
  )
  const size = Number(params.pageSize || 10)
  const start = (Number(params.pageNum || 1) - 1) * size
  return {
    total: filtered.length,
    pages: Math.ceil(filtered.length / size),
    list: filtered
      .slice(start, start + size)
      .map((row) => normalizeOrder(row, getHotelById?.(row.hotelId))),
  }
}

async function orderPage(fetcher, params) {
  if (useMock) {
    const service = await mock
    return mockOrderPage(service.listOrders(), params, service.getHotelById)
  }
  const result = await fetcher(params)
  return {
    total: Number(result.total),
    pages: Number(result.pages),
    list: pageList(result).map(normalizeOrder),
  }
}
export const getOrderPage = (params = {}) => orderPage(orderApi.getOrderList, params)
export const getMerchantOrderPage = (params = {}) =>
  orderPage(merchantApi.getMerchantOrders, params)
export const getAdminOrderPage = (params = {}) => orderPage(adminApi.getPlatformOrders, params)
export async function getBooking(id) {
  if (useMock) {
    const service = await mock
    const order = service.listOrders().find((row) => String(row.id) === String(id))
    return order ? normalizeOrder(order, service.getHotelById(order.hotelId)) : null
  }
  return normalizeOrder(await orderApi.getOrderDetail(id))
}
