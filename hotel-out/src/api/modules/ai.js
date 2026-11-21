import request from '../request'

// Status is public and optional; an unavailable assistant must not clear the login session.
export async function getAiStatus() {
  if (import.meta.env.VITE_USE_MOCK === 'true') return { enabled: false, mock: true }
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
  const response = await fetch(`${base}/ai/status`, { signal: AbortSignal.timeout(6000) })
  const result = await response.json()
  if (!response.ok || result.code !== 200) throw new Error('AI 服务暂时无法连接')
  return result.data
}

export const searchWithAi = (data) =>
  request.post('/ai/search', data, { timeout: data.mode === 'quick' ? 40_000 : 110_000 })
export const getAiSettings = () => request.get('/admin/ai/settings')
export const saveAiSettings = (data) => request.put('/admin/ai/settings', data)
export const testAiConnection = (data) => request.post('/admin/ai/test', data, { timeout: 40_000 })
export const listAiConnections = () => request.get('/admin/ai/connections')
export const saveAiConnection = (id, data) =>
  id
    ? request.put(`/admin/ai/connections/${id}`, data)
    : request.post('/admin/ai/connections', data)
export const activateAiConnection = (id) => request.put(`/admin/ai/connections/${id}/activate`)
export const deleteAiConnection = (id) => request.delete(`/admin/ai/connections/${id}`)
export const testSavedAiConnection = (id) =>
  request.post(`/admin/ai/connections/${id}/test`, {}, { timeout: 40_000 })
export const testAiDraft = (id, data) =>
  request.post('/admin/ai/connections/test', data, { params: id ? { id } : {}, timeout: 40_000 })
