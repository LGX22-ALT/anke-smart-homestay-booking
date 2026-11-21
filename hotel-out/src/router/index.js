import { createRouter, createWebHistory } from 'vue-router'

import { pinia, useUserStore } from '@/stores'

import adminRoutes from './modules/admin'
import clientRoutes from './modules/client'
import merchantRoutes from './modules/merchant'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...clientRoutes, ...merchantRoutes, ...adminRoutes],
  scrollBehavior: (to) => (to.hash ? { el: to.hash, top: 24 } : { top: 0 }),
})

router.beforeEach((to) => {
  const userStore = useUserStore(pinia)
  userStore.hydrateSession()

  if (to.meta.guestOnly && userStore.isAuthenticated) {
    const requestedPath = String(to.query.redirect || '')
    const safePath =
      requestedPath.startsWith('/') && !requestedPath.startsWith('//') ? requestedPath : ''

    if (safePath && safePath !== '/login') {
      return safePath
    }

    if (userStore.role === 'merchant') return { name: 'merchant-dashboard' }
    if (userStore.role === 'admin') return { name: 'admin-dashboard' }
    return { name: 'client-home' }
  }

  if (!to.meta.requiresAuth) {
    return true
  }

  if (!userStore.isAuthenticated) {
    return {
      name: 'client-login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  const allowedRoles = Array.isArray(to.meta.roles) ? to.meta.roles : []

  if (allowedRoles.length > 0 && !allowedRoles.includes(userStore.role)) {
    return {
      name: 'forbidden',
      query: {
        from: to.fullPath,
      },
    }
  }

  return true
})

router.afterEach((to) => {
  const appTitle = import.meta.env.VITE_APP_TITLE || '民宿酒店预订平台'
  document.title = to.meta.title ? `${to.meta.title} - ${appTitle}` : appTitle
})

export default router
