import { describe, expect } from 'vitest'
import { AssetMapping } from '@/api/__generated__'

import { compareStatus } from '@/modules/Pulse/utils/pagination-utils.ts'

interface TestEachSuite {
  a: AssetMapping.status | undefined
  b: AssetMapping.status | undefined
  expected: 0 | 1 | -1
}

describe('compareStatus', () => {
  test.each<TestEachSuite>([
    { a: undefined, b: undefined, expected: 0 },
    { a: undefined, b: AssetMapping.status.STREAMING, expected: -1 },
    { a: AssetMapping.status.UNMAPPED, b: undefined, expected: 0 },
    { a: AssetMapping.status.UNMAPPED, b: AssetMapping.status.UNMAPPED, expected: 0 },
    { a: AssetMapping.status.UNMAPPED, b: AssetMapping.status.DRAFT, expected: -1 },
    { a: AssetMapping.status.UNMAPPED, b: AssetMapping.status.STREAMING, expected: -1 },
    { a: AssetMapping.status.UNMAPPED, b: AssetMapping.status.REQUIRES_REMAPPING, expected: -1 },
    { a: AssetMapping.status.STREAMING, b: undefined, expected: 1 },
    { a: AssetMapping.status.STREAMING, b: AssetMapping.status.UNMAPPED, expected: 1 },
    { a: AssetMapping.status.STREAMING, b: AssetMapping.status.DRAFT, expected: 1 },
    { a: AssetMapping.status.STREAMING, b: AssetMapping.status.STREAMING, expected: 0 },
    { a: AssetMapping.status.STREAMING, b: AssetMapping.status.REQUIRES_REMAPPING, expected: -1 },
  ])('should returns $expected with $a and $b', ({ a, b, expected }) => {
    expect(compareStatus(a, b)).toStrictEqual(expected)
  })
})
