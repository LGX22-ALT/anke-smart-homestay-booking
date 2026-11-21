import { defineConfig } from '@playwright/test'

if (!process.env.E2E_BACKEND_URL)
  throw new Error('Run through BrowserJourneyTests to use an isolated database')

export default defineConfig({
  testDir: './e2e',
  workers: 1,
  timeout: 60_000,
  outputDir: 'test-results',
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:2299',
    screenshot: 'only-on-failure',
    trace: 'off', // Authentication headers and response bodies must not enter saved traces.
    launchOptions: process.env.E2E_CHROME_PATH
      ? { executablePath: process.env.E2E_CHROME_PATH }
      : {},
  },
  webServer: {
    command: 'npm run dev -- --port 2299 --strictPort',
    url: 'http://127.0.0.1:2299',
    reuseExistingServer: false,
    env: { VITE_BACKEND_URL: process.env.E2E_BACKEND_URL, VITE_USE_MOCK: 'false' },
  },
})
