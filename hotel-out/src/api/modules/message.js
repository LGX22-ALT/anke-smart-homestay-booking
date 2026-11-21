import request from '../request'

export const getMessageConversations = () => request.get('/messages')

export const getHotelMessages = (hotelId) => request.get(`/messages/hotels/${hotelId}`)
export const sendHotelMessage = (hotelId, data) => request.post(`/messages/hotels/${hotelId}`, data)
