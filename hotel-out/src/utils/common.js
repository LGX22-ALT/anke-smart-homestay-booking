export function debounce(callback, delay = 300) {
  let timerId

  function debounced(...args) {
    clearTimeout(timerId)
    timerId = setTimeout(() => callback.apply(this, args), delay)
  }

  debounced.cancel = () => clearTimeout(timerId)
  return debounced
}

export function throttle(callback, delay = 300) {
  let lastRunAt = 0
  let timerId

  function throttled(...args) {
    const elapsed = Date.now() - lastRunAt
    const invoke = () => {
      lastRunAt = Date.now()
      timerId = undefined
      callback.apply(this, args)
    }

    if (elapsed >= delay) {
      invoke()
      return
    }

    clearTimeout(timerId)
    timerId = setTimeout(invoke, delay - elapsed)
  }

  throttled.cancel = () => clearTimeout(timerId)
  return throttled
}

export function deepClone(value) {
  if (typeof structuredClone === 'function') {
    return structuredClone(value)
  }

  return JSON.parse(JSON.stringify(value))
}

const priceFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
})

export function formatPrice(value) {
  const price = Number(value)
  return Number.isFinite(price) && price >= 0 ? priceFormatter.format(price) : ''
}
