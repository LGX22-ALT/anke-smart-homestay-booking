import assert from 'node:assert/strict'
import test from 'node:test'
import { demoHotels } from '../src/constants/demoHotels.js'
import { additionalAttractions } from '../src/constants/destinations.js'
import { CITIES, getDistricts } from '../src/constants/locations.js'

const addedCities = ['南京', '重庆', '西安', '青岛', '三亚', '武汉', '苏州', '桂林']

test('expanded city catalog has hotel, attraction and district data', () => {
  for (const city of addedCities) {
    assert.ok(CITIES.includes(city), `${city} should be selectable`)
    assert.ok(getDistricts(city).length >= 3, `${city} should have districts`)
    assert.ok(demoHotels.filter((hotel) => hotel.city === city).length >= 2)
    assert.ok(additionalAttractions.filter((attraction) => attraction.city === city).length >= 3)
  }
})

test('new hotels keep the room shape required by search and booking cards', () => {
  const extraHotels = demoHotels.filter((hotel) => addedCities.includes(hotel.city))
  assert.equal(extraHotels.length, addedCities.length * 2)
  assert.ok(extraHotels.every((hotel) => hotel.rooms.length >= 2 && hotel.price > 0))
  assert.ok(extraHotels.every((hotel) => hotel.image && hotel.gallery.length > 0))
})
