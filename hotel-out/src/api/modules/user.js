import request from '../request'

export const login = (data) => request.post('/user/login', data)
export const register = (data) => request.post('/user/register', data)
export const refreshToken = (data) => request.post('/user/refresh', data)
export const logout = (data) => request.post('/user/logout', data)
export const getProfile = () => request.get('/user/profile')
export const updateProfile = (data) => request.put('/user/profile', data)
export const uploadAvatar = (file) => {
  const body = new FormData()
  body.append('file', file)
  return request.post('/user/avatar', body, { headers: { 'Content-Type': undefined } })
}
export const getFavorites = () => request.get('/user/favorites')
export const toggleFavorite = (hotelId) => request.post(`/user/favorites/${hotelId}`)
export const getComments = () => request.get('/user/comments')
export const createComment = (data) => request.post('/user/comments', data)
export const updateComment = (id, data) => request.put(`/user/comments/${id}`, data)
export const deleteComment = (id) => request.delete(`/user/comments/${id}`)

export const addFavorite = (id) => request.put(`/user/favorites/${id}`)
export const removeFavorite = (id) => request.delete(`/user/favorites/${id}`)
