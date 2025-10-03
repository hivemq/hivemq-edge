const componentToHex = (c: number) => {
  const hex = c.toString(16)
  return hex.length == 1 ? '0' + hex : hex
}

export const rgbToHex = (r: number, g: number, b: number) =>
  '#' + componentToHex(r) + componentToHex(g) + componentToHex(b)

export const hexToRgb = (hex: string, asString = false) => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  const format = result
    ? {
        r: Number.parseInt(result[1], 16),
        g: Number.parseInt(result[2], 16),
        b: Number.parseInt(result[3], 16),
      }
    : null
  if (!format) return null
  return asString ? `rgb(${format.r}, ${format.g}, ${format.b})` : format
}
