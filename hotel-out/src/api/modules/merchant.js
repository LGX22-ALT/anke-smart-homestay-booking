import request from '../request'
export const getMerchantRoomTypes = (id) => request.get(`/merchant/hotels/${id}/rooms`)

export const getMerchantDashboard = () => request.get('/merchant/dashboard')
export const getMerchantHotels = (params) => request.get('/merchant/hotels', { params })
export const saveMerchantHotel = (data) => request.post('/merchant/hotels', data)
export const updateMerchantHotel = (id, data) => request.put(`/merchant/hotels/${id}`, data)
export const updateMerchantHotelStatus = (id, status) =>
  request.put(`/merchant/hotels/${id}/status`, null, { params: { status } })
export const getRoomCalendar = (params) => request.get('/merchant/room-calendar', { params })
export const updateRoomCalendar = (data) => request.put('/merchant/room-calendar', data)
export const getMerchantOrders = (params) => request.get('/merchant/orders', { params })
export const confirmMerchantOrder = (id) => request.put(`/merchant/orders/${id}/confirm`)
export const checkInMerchantOrder = (id) => request.put(`/merchant/orders/${id}/check-in`)
export const cancelMerchantOrder = (id) => request.put(`/merchant/orders/${id}/cancel`)
export const getMerchantReviews = () => request.get('/merchant/reviews')
export const replyReview = (id, data) => request.put(`/merchant/reviews/${id}/reply`, data)
export const getMerchantStatistics = (params) => request.get('/merchant/statistics', { params })
export const getMessageConversations = () => request.get('/merchant/messages')
export const getMessageThread = (hotelId, userId) =>
  request.get(`/merchant/messages/${hotelId}/${userId}`)
export const replyMessage = (hotelId, userId, data) =>
  request.post(`/merchant/messages/${hotelId}/${userId}`, data)
