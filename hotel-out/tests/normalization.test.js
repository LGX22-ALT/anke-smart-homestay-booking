import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('../src/services/dataSource.js', import.meta.url), 'utf8')
const helper = source.slice(
  source.indexOf('function parseJson'),
  source.indexOf('export function normalizeOrder'),
)
const normalizeHotel = new Function(
  'hotelStatusText',
  helper.replace('export function normalizeHotel', 'function normalizeHotel') +
    '\nreturn normalizeHotel',
)({})

for (const value of ['broken-json', 'null', '{}', '"text"', '42']) {
  test(`hotel JSON arrays tolerate ${value}`, () => {
    const hotel = normalizeHotel({
      gallery: value,
      coverImage: '/cover.jpg',
      facilities: value,
      nearbyInfo: value,
      rooms: [{ facilities: value }],
    })
    assert.deepEqual(hotel.gallery, ['/cover.jpg'])
    assert.deepEqual(hotel.facilities, [])
    assert.deepEqual(hotel.nearby, [])
    assert.deepEqual(hotel.rooms[0].facilities, [])
  })
}
test('valid JSON arrays remain available', () => {
  const hotel = normalizeHotel({ gallery: '["/photo.jpg"]', facilities: '["WiFi"]' })
  assert.deepEqual(hotel.gallery, ['/photo.jpg'])
  assert.deepEqual(hotel.facilities, ['WiFi'])
})
