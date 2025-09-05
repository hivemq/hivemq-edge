import { expect } from 'vitest'
import { getRollingHash } from '@/components/rjsf/BatchModeMappings/utils/rolling-hash.utils.ts'

describe('getRollingHash', () => {
  it.each([
    ['v-97', 'a'],
    ['v-98', 'b'],
    ['v-96354', 'abc'],
    ['v-98274', 'cba'],
    ['xxxx-98274', 'cba', 'xxxx-'],
    ['v-420597824', 'cbacbacbacbacbacbacbacbacbacba'],
  ])('should return %s for %s', (a, b, c) => {
    expect(getRollingHash(b, c)).toBe(a)
  })
})
