import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat.js'

dayjs.extend(customParseFormat)

const MOBILE_PATTERN = /^1[3-9]\d{9}$/
const ID_CARD_PATTERN = /^\d{6}(\d{8}|\d{6})\d{3}[\dXx]$/
const PRICE_PATTERN = /^(0|[1-9]\d*)(\.\d{1,2})?$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function isValidMobile(value) {
  return MOBILE_PATTERN.test(String(value ?? '').trim())
}

export function isValidEmail(value) {
  return EMAIL_PATTERN.test(String(value ?? '').trim())
}

export function isValidIdCard(value) {
  const idCard = String(value ?? '').trim()

  if (!ID_CARD_PATTERN.test(idCard)) {
    return false
  }

  const birthDate = idCard.length === 18 ? idCard.slice(6, 14) : `19${idCard.slice(6, 12)}`
  return dayjs(birthDate, 'YYYYMMDD', true).isValid()
}

export function isValidPrice(value) {
  return PRICE_PATTERN.test(String(value ?? '').trim())
}
