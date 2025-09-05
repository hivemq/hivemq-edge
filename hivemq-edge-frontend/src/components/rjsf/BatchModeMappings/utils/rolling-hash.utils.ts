/**
 * Simple deterministic 31-based rolling hash of the given string
 */
export const getRollingHash = (str: string, prefix = 'v-') => {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i)
    hash |= 0
  }
  return `${prefix}${Math.abs(hash)}`
}
