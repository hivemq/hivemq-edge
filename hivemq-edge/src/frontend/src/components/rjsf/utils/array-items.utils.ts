const FORMAT_INDEX_MARKER = '#'
const FORMAT_SEPARATOR = '-'

export const formatItemName = (stub: string, index: number, description?: string) => {
  if (!description) return `${stub} ${FORMAT_INDEX_MARKER}${index}`
  return `${stub} ${FORMAT_INDEX_MARKER}${index} ${FORMAT_SEPARATOR} ${description}`
}
