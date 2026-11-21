import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { createPinia, setActivePinia } from 'pinia'

// Load the actual store with Node-resolvable imports; keep browser crypto behavior.
let source = await readFile(new URL('../src/stores/modules/booking.js', import.meta.url), 'utf8')
for (const name of ['vue', 'pinia'])
  source = source.replace(`'${name}'`, JSON.stringify(import.meta.resolve(name)))
source = source.replace(
  "'@/utils/date'",
  JSON.stringify(new URL('../src/utils/date.js', import.meta.url).href),
)
source = source.replace(
  "'@/utils/requestId'",
  JSON.stringify(new URL('../src/utils/requestId.js', import.meta.url).href),
)
const { useBookingStore } = await import(
  `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
)

test('booking works on HTTP without randomUUID and keeps its id until a new selection', (t) => {
  const descriptor = Object.getOwnPropertyDescriptor(globalThis.crypto, 'randomUUID')
  Object.defineProperty(globalThis.crypto, 'randomUUID', { value: undefined, configurable: true })
  t.after(() => {
    if (descriptor) Object.defineProperty(globalThis.crypto, 'randomUUID', descriptor)
    else delete globalThis.crypto.randomUUID
  })
  setActivePinia(createPinia())
  const store = useBookingStore()
  const booking = {
    hotel: { id: 1 },
    room: { id: 2 },
    checkInDate: '2026-12-01',
    checkOutDate: '2026-12-03',
    guestCount: 2,
  }
  store.setBooking(booking)
  const first = store.requestId
  assert.match(first, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.equal(store.hotel.id, 1)
  assert.equal(store.room.id, 2)
  assert.equal(store.stayNights, 2)
  assert.equal(store.requestId, first)
  store.setBooking({ ...booking, room: { id: 3 } })
  assert.notEqual(store.requestId, first)
  store.clearBooking()
  assert.equal(store.requestId, '')
})

test('booking also works with native randomUUID', () => {
  setActivePinia(createPinia())
  const store = useBookingStore()
  store.setBooking({ hotel: { id: 1 }, room: { id: 2 } })
  assert.match(store.requestId, /^[0-9a-f-]{36}$/)
})
