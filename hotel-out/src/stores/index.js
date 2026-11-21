import { createPinia } from 'pinia'

export const pinia = createPinia()

export { useAppStore } from './modules/app'
export { useBookingStore } from './modules/booking'
export { useUserStore } from './modules/user'
