import request from '../request'

export const getAdminDashboard = () => request.get('/admin/dashboard')
export const getAdminUsers = (params) => request.get('/admin/users', { params })
export const updateUserStatus = (id, status) =>
  request.put(`/admin/users/${id}/status`, null, { params: { status } })
export const resetUserPassword = (id) => request.post(`/admin/users/${id}/reset-password`)
export const getAdminMerchants = (params) => request.get('/admin/merchants', { params })
export const updateMerchantStatus = (id, status) =>
  request.put(`/admin/merchants/${id}/status`, null, { params: { status } })
export const auditHotel = (id, data) => request.put(`/admin/hotels/${id}/audit`, data)
export const getHotelAudits = (params) => request.get('/admin/hotels/audits', { params })
export const getPlatformOrders = (params) => request.get('/admin/orders', { params })
export const interveneOrder = (id, data) => request.put(`/admin/orders/${id}/intervene`, data)
export const getAdminReviews = (params) => request.get('/admin/reviews', { params })
export const moderateReview = (id, data) => request.put(`/admin/reviews/${id}`, data)
export const updateReviewUserStatus = (id, status) =>
  request.put(`/admin/reviews/${id}/user-status`, null, { params: { status } })
export const getBanners = () => request.get('/admin/banners')
export const createBanner = (data) => request.post('/admin/banners', data)
export const updateBanner = (id, data) => request.put(`/admin/banners/${id}`, data)
export const deleteBanner = (id) => request.delete(`/admin/banners/${id}`)
export const getSystemDicts = (params) => request.get('/admin/system-dicts', { params })
export const createSystemDict = (data) => request.post('/admin/system-dicts', data)
export const updateSystemDict = (id, data) => request.put(`/admin/system-dicts/${id}`, data)
export const deleteSystemDict = (id) => request.delete(`/admin/system-dicts/${id}`)
