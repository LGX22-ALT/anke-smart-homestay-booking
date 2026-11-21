import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import {
  createMemoryHistory,
  createRouter,
  isNavigationFailure,
  NavigationFailureType,
} from 'vue-router'

const raw = await readFile(new URL('../src/views/client/Search/index.vue', import.meta.url), 'utf8')
const source = raw
  .split('<script setup>')[1]
  .split('</script>')[0]
  .replace(/^import .*$/gm, '')

async function harness(query = {}) {
  let calls = 0
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/search', name: 'client-search', component: {} }],
  })
  await router.push({ name: 'client-search', query })
  const route = {
    get query() {
      return router.currentRoute.value.query
    },
    get fullPath() {
      return router.currentRoute.value.fullPath
    },
  }
  const appStore = {
    selectedCity: '北京',
    setSelectedCity(city) {
      this.selectedCity = city
    },
  }
  const setup = new Function(
    'dayjs',
    'computed',
    'reactive',
    'ref',
    'watch',
    'onMounted',
    'useRoute',
    'useRouter',
    'useAppStore',
    'getHotelPage',
    'getStayNights',
    'CITIES',
    'isNavigationFailure',
    'NavigationFailureType',
    source + '\nreturn { syncQuery, search, reset, filters, results }',
  )
  const state = setup(
    dayjs,
    computed,
    reactive,
    ref,
    (_getter, callback) =>
      router.afterEach((_to, _from, failure) => {
        if (!failure) callback()
      }),
    () => {},
    () => route,
    () => router,
    () => appStore,
    async () => {
      calls++
      return { list: [], total: 0, pages: 0 }
    },
    (start, end) => dayjs(end).diff(dayjs(start), 'day'),
    [],
    isNavigationFailure,
    NavigationFailureType,
  )
  state.syncQuery()
  return { ...state, calls: () => calls }
}

test('reset at bare search URL clears remembered city and reloads hotels', async () => {
  const h = await harness()
  await h.reset()
  assert.equal(h.filters.city, '')
  assert.equal(h.calls(), 1)
})

test('submitting identical search criteria refreshes results', async () => {
  const h = await harness({
    city: '北京',
    sort: 'score',
    checkIn: dayjs().format('YYYY-MM-DD'),
    checkOut: dayjs().add(1, 'day').format('YYYY-MM-DD'),
    pageNum: '1',
  })
  await h.search()
  assert.equal(h.calls(), 1)
})

test('identical search also refreshes when URL query parameters are reordered', async () => {
  const h = await harness({
    pageNum: '1',
    checkOut: dayjs().add(1, 'day').format('YYYY-MM-DD'),
    checkIn: dayjs().format('YYYY-MM-DD'),
    sort: 'score',
    city: '北京',
  })
  await h.search()
  assert.equal(h.calls(), 1)
})
