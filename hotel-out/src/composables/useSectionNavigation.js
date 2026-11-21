import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

export function useSectionNavigation(sections, contentKey) {
  const sectionNav = ref(null)
  const activeSection = ref(sections[0].id)
  const indicator = ref({ left: 0, width: 0 })
  const indicatorStyle = computed(() => ({
    width: `${indicator.value.width}px`,
    transform: `translateX(${indicator.value.left}px)`,
  }))
  let frame = 0
  let settleTimer = 0
  let scrollingTo = null
  const reducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches

  function measure() {
    const nav = sectionNav.value
    const button = nav?.querySelector(`[data-section="${activeSection.value}"]`)
    if (!button) return
    indicator.value = { left: button.offsetLeft, width: button.offsetWidth }
    if (
      button.offsetLeft < nav.scrollLeft ||
      button.offsetLeft + button.offsetWidth > nav.scrollLeft + nav.clientWidth
    )
      nav.scrollTo({
        left: Math.max(0, button.offsetLeft - (nav.clientWidth - button.offsetWidth) / 2),
        behavior: reducedMotion() ? 'auto' : 'smooth',
      })
  }
  function sync() {
    frame = 0
    if (!sectionNav.value) return
    if (scrollingTo) {
      if (Math.abs(window.scrollY - scrollingTo.top) < 3) {
        scrollingTo = null
        return
      }
      if (!scrollingTo.instant) return
      scrollingTo = null
    }
    const threshold = sectionNav.value.getBoundingClientRect().bottom + 28
    let current = sections[0].id
    for (const section of sections) {
      const target = document.getElementById(section.id)
      if (target && target.getBoundingClientRect().top <= threshold) current = section.id
    }
    if (window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 3) {
      current = sections[sections.length - 1].id
    }
    activeSection.value = current
    measure()
  }
  function onScroll() {
    clearTimeout(settleTimer)
    settleTimer = window.setTimeout(() => {
      scrollingTo = null
    }, 160)
    if (!frame) frame = window.requestAnimationFrame(sync)
  }
  function interruptScroll() {
    scrollingTo = null
  }
  function onKey(event) {
    if (['PageDown', 'PageUp', 'Home', 'End', 'ArrowDown', 'ArrowUp'].includes(event.key))
      interruptScroll()
  }
  function scrollToSection(id) {
    const target = document.getElementById(id)
    if (!target) return
    const instant = reducedMotion()
    const margin = Number.parseFloat(window.getComputedStyle(target).scrollMarginTop) || 0
    scrollingTo = {
      top: Math.max(
        0,
        Math.min(
          target.getBoundingClientRect().top + window.scrollY - margin,
          document.documentElement.scrollHeight - window.innerHeight,
        ),
      ),
      instant,
    }
    activeSection.value = id
    measure()
    target.scrollIntoView({ behavior: instant ? 'auto' : 'smooth', block: 'start' })
    clearTimeout(settleTimer)
    settleTimer = window.setTimeout(() => {
      scrollingTo = null
    }, 1000)
  }
  watch(activeSection, () => nextTick(measure))
  watch(
    contentKey,
    async () => {
      await nextTick()
      activeSection.value = sections[0].id
      measure()
    },
    { flush: 'post' },
  )
  onMounted(() => {
    nextTick(measure)
    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', measure)
    window.addEventListener('wheel', interruptScroll, { passive: true })
    window.addEventListener('touchstart', interruptScroll, { passive: true })
    window.addEventListener('keydown', onKey)
  })
  onBeforeUnmount(() => {
    window.cancelAnimationFrame(frame)
    window.clearTimeout(settleTimer)
    window.removeEventListener('scroll', onScroll)
    window.removeEventListener('resize', measure)
    window.removeEventListener('wheel', interruptScroll)
    window.removeEventListener('touchstart', interruptScroll)
    window.removeEventListener('keydown', onKey)
  })
  return { sectionNav, activeSection, indicatorStyle, scrollToSection }
}
