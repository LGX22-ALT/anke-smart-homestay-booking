import dayjs from 'dayjs'

export function formatDate(value, template = 'YYYY-MM-DD') {
  const date = dayjs(value)
  return date.isValid() ? date.format(template) : ''
}

export function getStayNights(checkInDate, checkOutDate) {
  const checkIn = dayjs(checkInDate).startOf('day')
  const checkOut = dayjs(checkOutDate).startOf('day')

  if (!checkIn.isValid() || !checkOut.isValid() || !checkOut.isAfter(checkIn)) {
    return 0
  }

  return checkOut.diff(checkIn, 'day')
}
