export const USER_ROLES = Object.freeze({
  USER: 'user',
  MERCHANT: 'merchant',
  ADMIN: 'admin',
})

export const VALID_USER_ROLES = Object.freeze(Object.values(USER_ROLES))

const TOKEN_KEY = 'hotel_token'
const REFRESH_TOKEN_KEY = 'hotel_refresh_token'
const USER_INFO_KEY = 'hotel_user_info'
const USER_ROLE_KEY = 'hotel_user_role'

function getStorage() {
  return typeof window === 'undefined' ? null : window.localStorage
}

export function getToken() {
  return getStorage()?.getItem(TOKEN_KEY) || ''
}

export function getSessionIdentity() {
  const user = getUserInfo()
  const role = getUserRole()
  return getToken() && user?.id != null && role ? `${role}:${user.id}` : ''
}

export function getRefreshToken() {
  return getStorage()?.getItem(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(refreshToken) {
  const normalizedToken = typeof refreshToken === 'string' ? refreshToken.trim() : ''
  if (!normalizedToken) throw new TypeError('refreshToken 不能为空')
  getStorage()?.setItem(REFRESH_TOKEN_KEY, normalizedToken)
  return normalizedToken
}

export function setToken(token) {
  const normalizedToken = typeof token === 'string' ? token.trim() : ''

  if (!normalizedToken) {
    throw new TypeError('token 不能为空')
  }

  getStorage()?.setItem(TOKEN_KEY, normalizedToken)
  return normalizedToken
}

export function getUserInfo() {
  const storage = getStorage()
  const storedValue = storage?.getItem(USER_INFO_KEY)

  if (!storedValue) {
    return null
  }

  try {
    const userInfo = JSON.parse(storedValue)
    return userInfo && typeof userInfo === 'object' && !Array.isArray(userInfo) ? userInfo : null
  } catch {
    storage.removeItem(USER_INFO_KEY)
    return null
  }
}

export function setUserInfo(userInfo) {
  if (!userInfo || typeof userInfo !== 'object' || Array.isArray(userInfo)) {
    throw new TypeError('userInfo 必须是对象')
  }

  getStorage()?.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  return userInfo
}

export function getUserRole() {
  const role = getStorage()?.getItem(USER_ROLE_KEY) || ''
  return VALID_USER_ROLES.includes(role) ? role : ''
}

export function setUserRole(role) {
  if (!VALID_USER_ROLES.includes(role)) {
    throw new TypeError('用户角色无效')
  }

  getStorage()?.setItem(USER_ROLE_KEY, role)
  return role
}

export function setAuthSession({ token, refreshToken, userInfo, role }) {
  const normalizedToken = typeof token === 'string' ? token.trim() : ''

  if (!normalizedToken || !userInfo || typeof userInfo !== 'object' || Array.isArray(userInfo)) {
    throw new TypeError('登录会话信息不完整')
  }

  if (!VALID_USER_ROLES.includes(role)) {
    throw new TypeError('用户角色无效')
  }

  setToken(normalizedToken)
  if (refreshToken) setRefreshToken(refreshToken)
  setUserInfo(userInfo)
  setUserRole(role)
}

export function clearAuthSession() {
  const storage = getStorage()

  storage?.removeItem(TOKEN_KEY)
  storage?.removeItem(REFRESH_TOKEN_KEY)
  storage?.removeItem(USER_INFO_KEY)
  storage?.removeItem(USER_ROLE_KEY)
}
