import { test, expect } from '@playwright/test'

test('malformed optional hotel fields do not break detail rendering', async ({ page }) => {
  const errors = []
  page.on('pageerror', (error) => errors.push(error.message))
  await page.route('**/api/hotels/1', async (route) => {
    const response = await route.fetch()
    const body = await response.json()
    body.data.hotel.gallery = '{}'
    body.data.hotel.facilities = 'null'
    body.data.hotel.nearbyInfo = '"text"'
    body.data.rooms[0].facilities = 'broken-json'
    await route.fulfill({ json: body })
  })
  await page.goto('/hotel/1')
  await expect(page.getByRole('heading', { name: 'Browser Stay', exact: true })).toBeVisible()
  await expect(page.locator('.gallery-empty')).toBeVisible()
  await expect(page.getByRole('button', { name: '预订此房型' })).toBeEnabled()
  expect(errors).toEqual([])
})

test('late unauthorized order response cannot replay as the newly signed in guest', async ({
  page,
}) => {
  await login(page, '13800000003', '/user/orders')
  let release
  let refreshes = 0
  const gate = new Promise((resolve) => {
    release = resolve
  })
  page.on('request', (request) => {
    if (request.url().endsWith('/api/user/refresh')) refreshes++
  })
  await page.route('**/api/orders', async (route) => {
    if (route.request().method() !== 'POST') return route.continue()
    await gate
    await route.fulfill({ status: 401, json: { code: 401, message: 'expired' } })
  })
  const sent = page.waitForRequest(
    (request) => request.url().endsWith('/api/orders') && request.method() === 'POST',
  )
  await page.evaluate(() => {
    window.audit4Result = null
    import('/src/api/request.js')
      .then(({ default: request }) => request.post('/orders', { hotelId: 1 }))
      .then(() => {
        window.audit4Result = 'unexpected success'
      })
      .catch((error) => {
        window.audit4Result = error.code
      })
  })
  await sent
  // Keep the original document and pending promise while replacing its account.
  await page.evaluate(async () => {
    const response = await fetch('/api/user/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '13800000004', password: 'BrowserTest@123' }),
    })
    const body = await response.json()
    const { setAuthSession } = await import('/src/utils/auth.js')
    setAuthSession(body.data)
  })
  release()
  await expect.poll(() => page.evaluate(() => window.audit4Result)).toBe('SESSION_CHANGED')
  expect(refreshes).toBe(0)
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem('hotel_user_info')).id)).toBe(4)
})

test.beforeEach(async ({ page }, testInfo) => {
  const failures = []
  const observe = (observed) => {
    observed.on('response', (response) => {
      if (response.status() >= 400)
        failures.push({ path: new URL(response.url()).pathname, status: response.status() })
    })
    observed.on('pageerror', (error) => failures.push({ error: error.message }))
  }
  observe(page)
  testInfo._observe = observe
  testInfo._networkFailures = failures
})
test.afterEach(async ({ browserName }, testInfo) => {
  if (testInfo.status !== testInfo.expectedStatus)
    await testInfo.attach('network-errors', {
      body: JSON.stringify({ browser: browserName, failures: testInfo._networkFailures }),
      contentType: 'application/json',
    })
})

async function login(page, phone, destination) {
  await page.goto(`/login?redirect=${encodeURIComponent(destination)}`)
  await page.locator('input[inputmode="numeric"]').fill(phone)
  await page.locator('input[type="password"]').fill('BrowserTest@123')
  await page.locator('form').getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL(new RegExp(destination.replaceAll('/', '\\/')))
}

test('guest books, merchant confirms, guest requests refund, admin refunds', async ({
  browser,
  baseURL,
}, testInfo) => {
  const guestContext = await browser.newContext({ baseURL })
  const guest = await guestContext.newPage()
  testInfo._observe(guest)
  await login(guest, '13800000003', '/search')
  await expect(guest.getByRole('heading', { name: 'Browser Stay' }).first()).toBeVisible()
  await guest.locator('.hotel-card').first().click()
  await guest.getByRole('button', { name: '预订此房型' }).first().click()
  await guest.getByRole('button', { name: '提交订单并支付' }).click()
  await expect(guest.getByRole('heading', { name: '预订成功' })).toBeVisible()
  const merchantContext = await browser.newContext({ baseURL })
  const merchant = await merchantContext.newPage()
  testInfo._observe(merchant)
  await login(merchant, '13800000002', '/merchant/orders')
  await merchant.getByRole('button', { name: '确认', exact: true }).click()
  await expect(merchant.getByRole('button', { name: '办理入住' })).toBeVisible()
  await guest.goto('/user/orders')
  await guest.getByRole('button', { name: '申请退款' }).click()
  await expect(guest.locator('.order-list')).toContainText('退款中')
  const adminContext = await browser.newContext({ baseURL })
  const admin = await adminContext.newPage()
  testInfo._observe(admin)
  await login(admin, '13800000001', '/admin/orders')
  await admin.getByRole('button', { name: '确认退款' }).click()
  await expect(admin.locator('tbody')).toContainText('已退款')
  await guest.reload()
  await expect(guest.locator('.order-list')).toContainText('已退款')
  await guest.goto('/admin/orders')
  await expect(guest).toHaveURL(/403/)
  await Promise.all([guestContext.close(), merchantContext.close(), adminContext.close()])
})

test('search loading, retry, rating and mobile layout', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/search?keyword=Browser')
  await expect(page.locator('.result-title strong')).toHaveText('暂无评分')
  await expect(page.locator('.result-card .image-placeholder')).toHaveText('商家暂未上传图片')
  let release
  const pending = new Promise((resolve) => {
    release = resolve
  })
  await page.route('**/api/hotels?**', async (route) => {
    await pending
    await route.continue()
  })
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('正在加载搜索结果')
  await expect(page.locator('.result-card')).toHaveCount(0)
  release()
  await expect(page.locator('.result-card')).toHaveCount(1)
  await page.unroute('**/api/hotels?**')
  await page.route('**/api/hotels?**', (route) =>
    route.fulfill({ status: 503, json: { code: 503, message: 'test unavailable' } }),
  )
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(page.getByRole('button', { name: '重新加载' })).toBeVisible()
  await page.unroute('**/api/hotels?**')
  await page.getByRole('button', { name: '重新加载' }).click()
  await expect(page.locator('.result-card')).toHaveCount(1)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await expect(page.locator('.el-message')).toHaveCount(0)
  await page.screenshot({ path: testInfo.outputPath('search-mobile.png'), fullPage: true })
})

test('expired payment cancels once and another guest cannot access it', async ({ page }) => {
  await login(page, '13800000003', '/user/orders')
  const hidden = await page.evaluate(async () => {
    const r = await fetch('/api/orders/900', {
      headers: { Authorization: `Bearer ${localStorage.getItem('hotel_token')}` },
    })
    return (await r.json()).code
  })
  expect(hidden).toBe(404)
  await page.evaluate(() => localStorage.clear())
  await page.goto('/login')
  await login(page, '13800000004', '/user/orders')
  for (let i = 0; i < 2; i++) {
    const result = await page.evaluate(async () => {
      const r = await fetch('/api/orders/900/pay/mock', {
        method: 'POST',
        headers: { Authorization: `Bearer ${localStorage.getItem('hotel_token')}` },
      })
      const body = await r.json()
      return { code: body.code, status: body.data?.orderStatus }
    })
    if (i === 0) expect(result.status).toBe('CANCELLED')
    else expect(result.code).toBe(1001)
  }
  await page.reload()
  await expect(page.locator('.order-list')).toContainText('已取消')
})
