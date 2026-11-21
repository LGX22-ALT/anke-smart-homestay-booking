import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendUrl = process.env.VITE_BACKEND_URL || env.VITE_BACKEND_URL || 'http://127.0.0.1:8080'

  return {
    // Baota serves the built site at the domain root; Vue Router uses this base too.
    base: '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '127.0.0.1',
      port: 2222,
      proxy: {
        '/api': { target: backendUrl, changeOrigin: true },
        '/uploads': { target: backendUrl, changeOrigin: true },
      },
    },
    preview: {
      host: '127.0.0.1',
      port: 4173,
    },
    define: {
      __APP_TITLE__: JSON.stringify(env.VITE_APP_TITLE || '民宿酒店预订平台'),
    },
  }
})
