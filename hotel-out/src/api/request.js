import axios from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import {
  clearAuthSession,
  getRefreshToken,
  getToken,
  setAuthSession,
  getSessionIdentity,
} from '@/utils/auth'

const DEFAULT_ERROR_MESSAGE = '系统繁忙，请稍后重试'
let unauthorizedRedirectPending = false
let refreshPromise = null

export class ApiError extends Error {
  constructor(message, options = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = options.code ?? null
    this.status = options.status ?? null
    this.response = options.response ?? null
    this.cause = options.cause
  }
}

function getErrorMessage(code, fallbackMessage) {
  const messages = {
    400: '请求参数有误，请检查后重试',
    401: '登录状态已失效，请重新登录',
    403: '当前账号没有访问权限',
    500: DEFAULT_ERROR_MESSAGE,
  }

  return fallbackMessage || messages[code] || DEFAULT_ERROR_MESSAGE
}

function redirectToLogin() {
  clearAuthSession()

  if (unauthorizedRedirectPending || router.currentRoute.value.name === 'client-login') {
    return
  }

  unauthorizedRedirectPending = true
  const redirect = router.currentRoute.value.fullPath || '/'

  router
    .replace({
      name: 'client-login',
      query: { redirect },
    })
    .finally(() => {
      unauthorizedRedirectPending = false
    })
}

const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

function isAuthEndpoint(config) {
  const url = String(config?.url || '')
  return ['/user/login', '/user/register', '/user/refresh'].some((path) => url.includes(path))
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new ApiError('登录状态已失效，请重新登录', { code: 401, status: 401 })
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post('/user/refresh', { refreshToken })
      .then((response) => {
        if (getRefreshToken() !== refreshToken) {
          throw new ApiError('登录会话已改变，请重新操作', { code: 'SESSION_CHANGED' })
        }
        const payload = response.data
        if (Number(payload?.code) !== 200 || !payload.data?.token) {
          throw new ApiError(payload?.message || '登录状态已失效，请重新登录', {
            code: Number(payload?.code) || 401,
            status: response.status,
            response,
          })
        }
        setAuthSession(payload.data)
        return payload.data.token
      })
      .catch((error) => {
        if (getRefreshToken() !== refreshToken) {
          throw new ApiError('登录会话已改变，请重新操作', { code: 'SESSION_CHANGED' })
        }
        throw error
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

async function retryAfterRefresh(config) {
  if (!config || config._retry || isAuthEndpoint(config)) {
    return rejectApiError({ code: 401, status: 401 })
  }
  config._retry = true
  let token
  try {
    token = await refreshAccessToken()
  } catch (error) {
    if (error.code === 'SESSION_CHANGED') throw error
    const response = error.response
    const code = Number(response?.data?.code ?? error.code)
    return rejectApiError({
      code: Number.isFinite(code) ? code : null,
      status: response?.status ?? error.status ?? null,
      message: response?.data?.message || (response ? '' : '登录状态暂时无法刷新，请重试'),
      response,
      cause: error,
    })
  }
  config.headers = config.headers || {}
  config.headers.Authorization = `Bearer ${token}`
  return request(config)
}

function rejectApiError({ code, status, message, response, cause }) {
  const normalizedMessage = getErrorMessage(code || status, message)

  if (code === 401 || status === 401) {
    redirectToLogin()
  }

  ElMessage({
    message: normalizedMessage,
    type: 'error',
    grouping: true,
  })
  return Promise.reject(
    new ApiError(normalizedMessage, {
      code,
      status,
      response,
      cause,
    }),
  )
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

function assertCurrentSession(config) {
  if (config && config._sessionIdentity !== getSessionIdentity()) {
    throw new ApiError('登录会话已改变，请重新操作', { code: 'SESSION_CHANGED' })
  }
}

request.interceptors.request.use((config) => {
  if (config._sessionIdentity === undefined) config._sessionIdentity = getSessionIdentity()
  assertCurrentSession(config)
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

request.interceptors.response.use(
  (response) => {
    assertCurrentSession(response.config)
    if (response.status === 204) {
      return null
    }

    const payload = response.data
    const code = Number(payload?.code)

    if (code === 200) {
      return payload.data
    }

    if (code === 401 && !isAuthEndpoint(response.config)) {
      return retryAfterRefresh(response.config)
    }

    return rejectApiError({
      code: Number.isFinite(code) ? code : null,
      status: response.status,
      message: payload?.message,
      response,
    })
  },
  (error) => {
    if (error.code === 'SESSION_CHANGED') return Promise.reject(error)
    assertCurrentSession(error.config)
    const response = error.response
    const status = response?.status ?? null
    const businessCode = Number(response?.data?.code)

    if ((status === 401 || businessCode === 401) && !isAuthEndpoint(error.config)) {
      return retryAfterRefresh(error.config)
    }

    return rejectApiError({
      code: Number.isFinite(businessCode) ? businessCode : null,
      status,
      message: response?.data?.message || (response ? '' : '网络连接失败，请检查网络后重试'),
      response,
      cause: error,
    })
  },
)

export default request
