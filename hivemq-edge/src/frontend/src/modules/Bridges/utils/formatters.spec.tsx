import { describe, it, expect } from 'vitest'
import { formatHost } from './formatters.tsx'

describe('formatHost', () => {
  it('should return null when the host is empty', () => {
    expect(formatHost('')).toBe('')
  })
  it('should not change the name if short enough', () => {
    expect(formatHost('name')).toBe('name')
  })
  it('should shorten the name if longer than minimum', () => {
    expect(formatHost('long_name')).toBe('long_na[...]')
  })
  it('should shorten the name but not the rest of the dot-separated string', () => {
    expect(formatHost('long_name.the_rest')).toBe('long_na[...].the_rest')
  })
})
