import request from '../request'

export const getHotelList = (params) => request.get('/hotels', { params })
export const getHotelDetail = (id) => request.get(`/hotels/${id}`)
export const getHotelReviews = (id, params) => request.get(`/hotels/${id}/reviews`, { params })
export const checkRoomAvailability = (id, params) =>
  request.get(`/hotels/${id}/availability`, { params })
