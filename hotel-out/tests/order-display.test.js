import assert from 'node:assert/strict'
import test from 'node:test'

import { toOrderDisplay } from '../src/utils/orderDisplay.js'

test('order display model combines order, room and hotel details', () => {
  const display = toOrderDisplay(
    {
      id: 'HO-1',
      hotelId: 12,
      roomTypeId: 1201,
      orderStatus: 'PAID',
      totalAmount: 688,
      checkIn: '2026-11-21',
      checkOut: '2026-11-23',
      guestName: '林先生',
      guestPhone: '13800138000',
    },
    {
      id: 12,
      name: '云栖江景民宿',
      address: '广州市海珠区江南大道',
      image: '/hotel.jpg',
      rooms: [
        {
          id: 1201,
          name: '江景大床房',
          bed: '1.8米大床',
          area: 36,
          guests: 2,
          facilities: ['Wi-Fi', '独立卫浴'],
        },
      ],
    },
  )

  assert.equal(display.hotelName, '云栖江景民宿')
  assert.equal(display.hotelImage, '/hotel.jpg')
  assert.equal(display.roomName, '江景大床房')
  assert.equal(display.roomArea, 36)
  assert.deepEqual(display.roomFacilities, ['Wi-Fi', '独立卫浴'])
  assert.equal(display.statusText, '待确认')
  assert.equal(display.amount, 688)
})

test('order display model safely handles incomplete records', () => {
  const display = toOrderDisplay({ id: 'HO-2', orderStatus: 'UNKNOWN' })

  assert.equal(display.hotelName, '未命名房源')
  assert.equal(display.roomName, '房型信息待确认')
  assert.deepEqual(display.roomFacilities, [])
  assert.equal(display.statusText, 'UNKNOWN')
})

test('legacy Chinese status text keeps the backend status code for filters', () => {
  const display = toOrderDisplay({ id: 'HO-3', status: '待入住' })

  assert.equal(display.orderStatus, 'PENDING_CHECKIN')
  assert.equal(display.statusText, '待入住')
})
