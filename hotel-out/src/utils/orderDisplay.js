const ORDER_STATUS_TEXT = {
  PENDING_PAYMENT: '待支付',
  PAID: '待确认',
  PENDING_CHECKIN: '待入住',
  CONFIRMED: '待入住',
  CHECKED_IN: '入住中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款处理中',
  REFUNDED: '已退款',
  PLATFORM_INTERVENED: '平台处理中',
}
const ORDER_STATUS_CODE = Object.entries(ORDER_STATUS_TEXT).reduce((result, [code, text]) => {
  if (!result[text]) result[text] = code
  return result
}, {})

function asList(value) {
  if (Array.isArray(value)) return value.filter(Boolean)
  if (typeof value !== 'string' || !value.trim()) return []
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.filter(Boolean)
  } catch {
    // Some legacy records store a comma separated facility string.
  }
  return value
    .split(/[,，、|]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function firstValue(...values) {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

export function toOrderDisplay(order = {}, hotel = null) {
  const room =
    order.room ||
    order.roomType ||
    hotel?.rooms?.find((item) => String(item.id) === String(order.roomTypeId)) ||
    hotel?.rooms?.[0] ||
    {}
  const hotelName = firstValue(
    order.hotel,
    order.hotelName,
    hotel?.name,
    `房源 #${order.hotelId || '-'}`,
  )
  const statusCode = firstValue(order.orderStatus, ORDER_STATUS_CODE[order.status], order.status)
  return {
    ...order,
    orderNo: firstValue(order.orderNo, order.id, '待生成'),
    hotelName: hotelName === `房源 #${order.hotelId || '-'}` ? '未命名房源' : hotelName,
    hotelId: order.hotelId || hotel?.id,
    hotelImage: firstValue(
      order.hotelImage,
      order.coverImage,
      order.image,
      hotel?.image,
      hotel?.coverImage,
      order.roomImage,
      room.image,
    ),
    hotelAddress: firstValue(
      order.hotelAddress,
      order.address,
      hotel?.address,
      [hotel?.city, hotel?.district].filter(Boolean).join(' · '),
      '地址信息待确认',
    ),
    roomName: firstValue(order.roomName, order.roomTypeName, room.name, '房型信息待确认'),
    roomBed: firstValue(order.roomBed, order.bed, room.bed, '床型待确认'),
    roomArea: firstValue(order.roomArea, order.area, room.area),
    roomGuests: firstValue(order.roomGuests, order.maxGuests, room.guests, room.maxGuests),
    roomFacilities: asList(firstValue(order.roomFacilities, order.facilities, room.facilities)),
    guestName: firstValue(order.guestName, order.guest, '入住人待确认'),
    guestPhone: firstValue(order.guestPhone, order.phone, '联系方式待确认'),
    checkIn: firstValue(order.checkIn, order.checkInDate, '待确认'),
    checkOut: firstValue(order.checkOut, order.checkOutDate, '待确认'),
    checkInTime: firstValue(order.checkInTime, '14:00'),
    checkOutTime: firstValue(order.checkOutTime, '12:00'),
    amount: Number(firstValue(order.amount, order.totalAmount, 0)),
    nights: Number(firstValue(order.nights, 0)),
    orderStatus: statusCode,
    statusText: ORDER_STATUS_TEXT[statusCode] || firstValue(order.status, statusCode, '待处理'),
    createdAt: firstValue(order.createdAt, order.createTime, '时间待确认'),
  }
}

export { ORDER_STATUS_TEXT }
