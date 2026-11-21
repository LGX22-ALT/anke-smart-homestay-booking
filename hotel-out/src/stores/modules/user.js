import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  VALID_USER_ROLES,
  clearAuthSession,
  getToken,
  getRefreshToken,
  getUserInfo,
  getUserRole,
  setAuthSession,
  setUserInfo,
  getSessionIdentity,
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)
  const role = ref('')

  const isAuthenticated = computed(
    () => Boolean(token.value) && Boolean(userInfo.value) && VALID_USER_ROLES.includes(role.value),
  )

  function clearSession() {
    clearAuthSession()
    token.value = ''
    userInfo.value = null
    role.value = ''
  }

  function hydrateSession() {
    const storedToken = getToken()
    const storedUserInfo = getUserInfo()
    const storedRole = getUserRole()

    if (!storedToken && !storedUserInfo && !storedRole) {
      clearSession()
      return false
    }

    if (!storedToken || !storedUserInfo || !VALID_USER_ROLES.includes(storedRole)) {
      clearSession()
      return false
    }

    token.value = storedToken
    userInfo.value = storedUserInfo
    role.value = storedRole
    return true
  }

  function setSession(session) {
    setAuthSession(session)
    token.value = session.token.trim()
    userInfo.value = session.userInfo
    role.value = session.role
  }

  function syncSession() {
    hydrateSession()
  }

  function updateProfileInfo(profile, expectedIdentity) {
    if (
      !isAuthenticated.value ||
      getSessionIdentity() !== expectedIdentity ||
      String(profile?.id) !== String(userInfo.value?.id)
    )
      return false
    userInfo.value = { ...userInfo.value, ...profile }
    setUserInfo(userInfo.value)
    return true
  }

  hydrateSession()

  return {
    token,
    userInfo,
    role,
    isAuthenticated,
    setSession,
    syncSession,
    updateProfileInfo,
    getRefreshToken,
    hydrateSession,
    clearSession,
  }
})
