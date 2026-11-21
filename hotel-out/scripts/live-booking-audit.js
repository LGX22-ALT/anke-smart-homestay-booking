import { chromium } from '@playwright/test'
import { mkdir, writeFile } from 'node:fs/promises'

// Read-only production audit: never log in, submit an order, or record credentials.
const out = new URL('../test-artifacts/live-booking/', import.meta.url)
await mkdir(out, { recursive: true })
const browser = await chromium.launch({
  executablePath:
    process.env.E2E_CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  args: ['--no-proxy-server'],
})
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
const evidence = {
  checkedAt: new Date().toISOString(),
  errors: [],
  consoleErrors: [],
  failedResponses: [],
  checks: [],
}
page.on('pageerror', (e) => evidence.errors.push(e.message))
page.on('console', (m) => {
  if (m.type() === 'error') evidence.consoleErrors.push(m.text())
})
page.on('response', (r) => {
  if (r.status() >= 400)
    evidence.failedResponses.push({ path: new URL(r.url()).pathname, status: r.status() })
})
try {
  for (const path of ['/', '/search', '/attractions', '/login', '/hotel/1']) {
    const r = await page.goto(`http://blog.liangguoxi.icu${path}`, {
      waitUntil: 'domcontentloaded',
    })
    await page.locator('main, form').first().waitFor({ timeout: 20000 })
    evidence.checks.push({ path, status: r.status(), title: await page.title() })
    if (path === '/login')
      evidence.login = await page.evaluate(() => ({
        demoButtons: document.querySelectorAll('.demo-accounts button').length,
        phonePrefilled: Boolean(document.querySelector('input[inputmode="numeric"]')?.value),
        passwordPrefilled: Boolean(document.querySelector('input[type="password"]')?.value),
      }))
  }
  const button = page.getByRole('button', { name: '预订此房型', exact: true }).first()
  await button.waitFor()
  await page.waitForFunction(() =>
    [...document.querySelectorAll('button')].some(
      (b) => b.textContent.includes('预订此房型') && !b.disabled,
    ),
  )
  evidence.environment = await page.evaluate(() => ({
    secure: isSecureContext,
    uuid: typeof crypto.randomUUID,
    randomValues: typeof crypto.getRandomValues,
  }))
  evidence.before = page.url()
  await button.click()
  // This audit captures failure as evidence, not a passing assertion.
  try {
    await page.waitForURL('**/login?**', { timeout: 4000 })
    evidence.reserveNavigated = true
  } catch {
    evidence.reserveNavigated = false
  }
  evidence.after = page.url()
  await page.screenshot({
    path: new URL('production-reserve.png', out).pathname.replace(/^\/([A-Za-z]:)/, '$1'),
    fullPage: true,
  })
  evidence.mobile = []
  await page.setViewportSize({ width: 390, height: 844 })
  for (const path of ['/search', '/hotel/1', '/login']) {
    await page.goto(`http://blog.liangguoxi.icu${path}`, { waitUntil: 'domcontentloaded' })
    await page.locator('main, form').first().waitFor()
    evidence.mobile.push({
      path,
      ...(await page.evaluate(() => ({
        viewport: innerWidth,
        content: document.documentElement.scrollWidth,
      }))),
    })
  }
} catch (e) {
  evidence.auditError = e.message
} finally {
  await writeFile(new URL('browser-before.json', out), JSON.stringify(evidence, null, 2))
  console.log(JSON.stringify(evidence))
  await browser.close()
}
