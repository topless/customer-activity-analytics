/** Formatting helpers shared across the app. */

// en-CH: Swiss grouping (12’345.67) — fitting for the domain and unambiguous.
const amountFormat = new Intl.NumberFormat('en-CH', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const scoreFormat = new Intl.NumberFormat('en-CH', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 1,
})

const intFormat = new Intl.NumberFormat('en-CH')

const dateFormat = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

/** `12’345.67` — pair with a currency code in the UI. */
export function formatAmount(amount: number): string {
  return amountFormat.format(amount)
}

/** Risk scores: integers plain, otherwise one decimal (`125.5`). */
export function formatScore(score: number): string {
  return scoreFormat.format(score)
}

export function formatInt(n: number): string {
  return intFormat.format(n)
}

/** `12 Aug 2026` */
export function formatDate(iso: string): string {
  return dateFormat.format(new Date(iso))
}

/** `12 Aug 2026, 14:03` */
export function formatDateTime(iso: string): string {
  return dateTimeFormat.format(new Date(iso))
}

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/** `2026-03` → `Mar` (or `Mar ’26` when `withYear`). Parsed manually to avoid TZ pitfalls. */
export function formatMonth(yearMonth: string, withYear = false): string {
  const [year, month] = yearMonth.split('-')
  const name = MONTH_NAMES[Number(month) - 1] ?? yearMonth
  return withYear ? `${name} ’${(year ?? '').slice(2)}` : name
}

/** `bc1qxyz…abcd` — keeps head and tail of long opaque strings (wallets, hashes). */
export function middleEllipsis(value: string, head = 10, tail = 6): string {
  if (value.length <= head + tail + 1) return value
  return `${value.slice(0, head)}…${value.slice(-tail)}`
}
