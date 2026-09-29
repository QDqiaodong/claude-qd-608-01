/**
 * 录入单位切换的<b>精确十进制移位</b>工具（仅用于把已录入的写法在 cm/mm 间换算）。
 *
 * 注意：这里不是放码计算，放码始终由服务端引擎完成。
 * cm↔mm 只是 ×10 / ÷10，对有限十进制字符串做精确移位（不经过 Number/float，无二进制误差）。
 * 真正的结果数值以服务端返回的微米口径为准，页面切换单位只取 cm/mm 现成字段渲染。
 */

/** 字符串十进制 × 10（精确）；非法输入原样返回。 */
export function timesTen(text) {
  if (text === null || text === undefined || String(text).trim() === '') return text
  let s = String(text).trim()
  const neg = s.startsWith('-')
  if (neg) s = s.slice(1)
  const [intPart, fracPart = ''] = s.split('.')
  if (fracPart.length <= 1) {
    // 0.3 → 3；90.25 走 else
    const merged = intPart + (fracPart || '')
    return (neg ? '-' : '') + (fracPart.length === 1 ? merged : merged + '0')
  }
  // 多位小数：把小数点右移一位
  const newFrac = fracPart.slice(1)
  const merged = intPart + fracPart[0]
  return (neg ? '-' : '') + trimLeadingZeros(merged) + '.' + newFrac
}

/** 字符串十进制 ÷ 10（精确）。 */
export function divideTen(text) {
  if (text === null || text === undefined || String(text).trim() === '') return text
  let s = String(text).trim()
  const neg = s.startsWith('-')
  if (neg) s = s.slice(1)
  const [intPart, fracPart = ''] = s.split('.')
  if (intPart.length <= 1) {
    const head = intPart.length === 1 ? '0.' + intPart : '0.0'
    return (neg ? '-' : '') + head + fracPart
  }
  const newInt = intPart.slice(0, -1)
  const moved = intPart.slice(-1)
  return (neg ? '-' : '') + newInt + '.' + moved + fracPart
}

function trimLeadingZeros(digits) {
  let i = 0
  while (i < digits.length - 1 && digits[i] === '0') i++
  return digits.slice(i)
}

/** 按当前显示单位取服务端结果字段（服务端同时给了 cm/mm，零计算）。 */
export function cellValue(cell, unit) {
  return unit === 'mm' ? cell.mm : cell.cm
}

export function deltaValue(cell, unit) {
  return unit === 'mm' ? cell.deltaMm : cell.deltaCm
}
