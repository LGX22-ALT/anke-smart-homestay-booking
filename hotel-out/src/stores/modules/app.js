import { defineStore } from 'pinia'
import { ref } from 'vue'
import { CITIES } from '@/constants/locations'

export const useAppStore = defineStore('app', () => {
  const title = ref(import.meta.env.VITE_APP_TITLE || '民宿酒店预订平台')
  const theme = ref('light')
  const locale = ref('zh-CN')
  const sidebarCollapsed = ref(false)
  function readCity() {
    try {
      const city = localStorage.getItem('hotel_selected_city') || ''
      return CITIES.includes(city) ? city : ''
    } catch {
      return ''
    }
  }
  const selectedCity = ref(readCity())
  function setSelectedCity(city) {
    selectedCity.value = CITIES.includes(city) ? city : ''
    try {
      localStorage.setItem('hotel_selected_city', selectedCity.value)
    } catch {
      /* Keep city selection available when browser storage is disabled. */
    }
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(collapsed) {
    sidebarCollapsed.value = Boolean(collapsed)
  }

  return {
    title,
    selectedCity,
    setSelectedCity,
    theme,
    locale,
    sidebarCollapsed,
    toggleSidebar,
    setSidebarCollapsed,
  }
})
