import request from '../request'

export const createOrder = (data) => request.post('/orders', data)
export const getOrderList = (params) => request.get('/orders', { params })
export const getOrderDetail = (id) => request.get(`/orders/${id}`)
export const cancelOrder = (id, data) => request.put(`/orders/${id}/cancel`, null, { params: data })
export const refundOrder = (id) => request.post(`/orders/${id}/refund`)
export const mockPayOrder = (id, success = true) =>
  request.post(`/orders/${id}/pay/mock`, null, { params: { success } })
