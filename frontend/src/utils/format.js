/** 金额：两位小数 + 千分位 */
export function yuan(value) {
  if (value === null || value === undefined || value === '') return '-'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 数值：默认保留一位小数（尺寸用） */
export function num(value, digits = 1) {
  if (value === null || value === undefined || value === '') return '-'
  return Number(value).toFixed(digits)
}

/** yyyy-MM-dd */
export function day(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

/** yyyy-MM-dd HH:mm */
export function dt(value) {
  if (!value) return '-'
  const text = String(value)
  const date = text.slice(0, 10)
  const time = text.slice(11, 16)
  return time ? `${date} ${time}` : date
}

/** 相对今天的天数（正数=未来） */
export function daysFromToday(value) {
  if (!value) return null
  const target = new Date(`${String(value).slice(0, 10)}T00:00:00`)
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  return Math.round((target - now) / 86400000)
}

export function todayStr() {
  return new Date().toISOString().slice(0, 10)
}

export function addDaysStr(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}
