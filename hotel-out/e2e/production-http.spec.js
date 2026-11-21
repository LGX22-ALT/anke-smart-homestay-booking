import { test, expect } from '@playwright/test'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

test('production bundle can reserve on an actual insecure HTTP origin', async ({
  browser,
}, testInfo) => {
  const dist = fileURLToPath(new URL('../dist/', import.meta.url))
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } })
  const page = await context.newPage()
  const errors = []
  page.on('pageerror', (error) => errors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text())
  })
  // Serve the release bytes on a genuinely insecure origin and proxy to the isolated test DB.
  // This is browser transport simulation, not a claim that cloud Nginx was changed.
  await context.route('http://booking.test/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (url.pathname.startsWith('/api/')) {
      const response = await context.request.fetch(
        `${process.env.E2E_BACKEND_URL}${url.pathname}${url.search}`,
        {
          method: request.method(),
          headers: request.headers(),
          data: request.postDataBuffer() ?? undefined,
        },
      )
      return route.fulfill({ response })
    }
    const file = url.pathname.startsWith('/assets/')
      ? path.resolve(dist, `.${url.pathname}`)
      : path.join(dist, 'index.html')
    if (!file.startsWith(dist)) return route.abort()
    const mime = { '.js': 'application/javascript', '.css': 'text/css', '.html': 'text/html' }
    try {
      await route.fulfill({
        body: await readFile(file),
        contentType: mime[path.extname(file)] || 'application/octet-stream',
      })
    } catch {
      await route.fulfill({ status: 404, body: '' })
    }
  })
  try {
    await page.goto('http://booking.test/hotel/1')
    expect(
      await page.evaluate(() => ({ secure: isSecureContext, uuid: typeof crypto.randomUUID })),
    ).toEqual({ secure: false, uuid: 'undefined' })
    const reserve = page.getByRole('button', { name: '预订此房型', exact: true }).first()
    await expect(reserve).toBeEnabled()
    await reserve.click()
    await expect(page).toHaveURL(/\/login\?redirect=/)
    await expect(page.locator('input[inputmode="numeric"]')).toHaveValue('')
    await expect(page.locator('input[type="password"]')).toHaveValue('')
    await expect(page.locator('.demo-accounts')).toHaveCount(0)
    await page.locator('input[inputmode="numeric"]').fill('13800000003')
    await page.locator('input[type="password"]').fill('BrowserTest@123')
    const loginResponse = page.waitForResponse('**/api/user/login')
    await page.locator('form').getByRole('button', { name: '登录', exact: true }).click()
    const response = await loginResponse
    expect(response.status(), 'Login HTTP status in isolated HTTP transport').toBe(200)
    expect((await response.json()).code, 'Login business status in isolated HTTP transport').toBe(
      200,
    )
    await expect(page).toHaveURL('http://booking.test/order/confirm')
    await expect(page.getByRole('button', { name: '提交订单并支付' })).toBeEnabled()
    await expect(page.locator('main')).toContainText('Browser Room')
    await expect(page.locator('.summary .image-placeholder')).toHaveText('商家暂未上传图片')
    await expect(page.locator('.summary img')).toHaveCount(0)
    await page.screenshot({ path: testInfo.outputPath('http-booking-confirm.png'), fullPage: true })
    expect(errors).toEqual([])
  } finally {
    await context.close()
  }
})
