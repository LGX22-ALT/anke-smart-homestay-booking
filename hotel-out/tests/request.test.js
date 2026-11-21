import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import axios from 'axios'

const raw = await readFile(new URL('../src/api/request.js', import.meta.url), 'utf8')
const source = raw
  .replace(/import axios from 'axios'\s*/, '')
  .replace(/import \{ ElMessage \} from 'element-plus'\s*/, '')
  .replace(/import router from '@\/router'\s*/, '')
  .replace(/import \{[\s\S]*?\} from '@\/utils\/auth'\s*/, '')
  .replaceAll('import.meta.env.VITE_API_BASE_URL', "'/api'")
  .replace('export class ApiError', 'class ApiError')
  .replace('export default request', 'return request')

function harness({
  firstStatus = 401,
  retryStatus = 500,
  refreshStatus = 200,
  businessUnauthorized = false,
  onRefresh = () => {},
  onFirstResponse = () => {},
}) {
  let access = 'expired'
  let refresh = 'refresh-token'
  let redirects = 0
  let refreshes = 0
  let attempts = 0
  let identity = 'user:3'
  const client = {
    create(options) {
      return axios.create({
        ...options,
        adapter: async (config) => {
          const isRefresh = config.url === '/user/refresh'
          const status = isRefresh ? refreshStatus : ++attempts === 1 ? firstStatus : retryStatus
          if (isRefresh) {
            refreshes++
            await onRefresh()
          }
          if (!isRefresh && attempts === 1) await onFirstResponse()
          const httpStatus = businessUnauthorized && !isRefresh && attempts === 1 ? 200 : status
          const response = {
            config,
            status: httpStatus,
            statusText: '',
            headers: {},
            data: {
              code: status,
              message: status === 500 ? 'server unavailable' : '',
              data:
                isRefresh && status === 200
                  ? { token: 'renewed', refreshToken: 'rotated', role: 'user', userInfo: { id: 3 } }
                  : [],
            },
          }
          if (httpStatus >= 400)
            throw new axios.AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, null, response)
          return response
        },
      })
    },
  }
  const request = new Function(
    'axios',
    'ElMessage',
    'router',
    'clearAuthSession',
    'getRefreshToken',
    'getToken',
    'setAuthSession',
    'getSessionIdentity',
    source,
  )(
    client,
    () => {},
    {
      currentRoute: { value: { name: 'user-orders', fullPath: '/user/orders' } },
      replace: async () => {
        redirects++
      },
    },
    () => {
      access = ''
      refresh = ''
    },
    () => refresh,
    () => access,
    (session) => {
      access = session.token
      refresh = session.refreshToken
    },
    () => identity,
  )
  return {
    request,
    changeSession: (token, refreshToken) => {
      access = token
      refresh = refreshToken
      identity = token ? 'user:4' : ''
    },
    state: () => ({ access, refresh, redirects, refreshes, attempts }),
  }
}

for (const status of [200, 401, 403, 500]) {
  test(`old response ${status} cannot affect another account or replay its request`, async () => {
    const h = harness({
      firstStatus: status,
      onFirstResponse: () => h.changeSession('new-account', 'new-refresh'),
    })
    await assert.rejects(
      h.request.post('/orders', { hotelId: 1 }),
      (error) => error.code === 'SESSION_CHANGED',
    )
    assert.equal(h.state().attempts, 1)
    assert.equal(h.state().refreshes, 0)
    assert.equal(h.state().redirects, 0)
    assert.equal(h.state().access, 'new-account')
  })
}

test('successful refresh followed by 500 preserves error and valid session', async () => {
  const h = harness({})
  await assert.rejects(
    h.request.get('/orders'),
    (error) => error.code === 500 && error.status === 500,
  )
  assert.equal(h.state().access, 'renewed')
  assert.equal(h.state().redirects, 0)
  assert.equal(h.state().refreshes, 1)
})
test('successful refresh followed by forbidden error preserves session', async () => {
  const h = harness({ retryStatus: 403 })
  await assert.rejects(h.request.get('/orders'), (error) => error.code === 403)
  assert.equal(h.state().access, 'renewed')
  assert.equal(h.state().redirects, 0)
})
test('business 401 followed by successful refresh and server error preserves session', async () => {
  const h = harness({ businessUnauthorized: true })
  await assert.rejects(
    h.request.get('/orders'),
    (error) => error.code === 500 && error.status === 500,
  )
  assert.equal(h.state().access, 'renewed')
  assert.equal(h.state().redirects, 0)
})
test('invalid refresh session redirects once and stops retrying', async () => {
  const h = harness({ refreshStatus: 401 })
  await assert.rejects(h.request.get('/orders'), (error) => error.code === 401)
  assert.equal(h.state().access, '')
  assert.equal(h.state().redirects, 1)
  assert.equal(h.state().attempts, 1)
})
test('transient refresh failure keeps the session available for retry', async () => {
  const h = harness({ refreshStatus: 503 })
  await assert.rejects(h.request.get('/orders'), (error) => error.status === 503)
  assert.equal(h.state().access, 'expired')
  assert.equal(h.state().refresh, 'refresh-token')
  assert.equal(h.state().redirects, 0)
})
test('a second unauthorized response does not loop token refresh', async () => {
  const h = harness({ retryStatus: 401 })
  await assert.rejects(h.request.get('/orders'), (error) => error.code === 401)
  assert.equal(h.state().refreshes, 1)
  assert.equal(h.state().attempts, 2)
})

for (const [label, token, refreshToken] of [
  ['logout', '', ''],
  ['account switch', 'new-account', 'new-account-refresh'],
]) {
  for (const refreshStatus of [200, 401, 503]) {
    test(`refresh ${refreshStatus} cannot overwrite session after ${label}`, async () => {
      const h = harness({ refreshStatus, onRefresh: () => h.changeSession(token, refreshToken) })
      await assert.rejects(h.request.get('/orders'), (error) => error.code === 'SESSION_CHANGED')
      assert.equal(h.state().access, token)
      assert.equal(h.state().refresh, refreshToken)
      assert.equal(h.state().attempts, 1)
      assert.equal(h.state().redirects, 0)
    })
  }
}
