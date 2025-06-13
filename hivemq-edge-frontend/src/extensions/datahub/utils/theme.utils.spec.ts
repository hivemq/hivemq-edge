import { describe, expect } from 'vitest'
import { getHandlePosition } from '@datahub/utils/theme.utils.ts'

interface Suite {
  index: number
  expected: string
}

const validationSuite = [
  {
    index: 0,
    expected: 'calc(var(--chakra-space-3) + var(--chakra-sizes-12) + 12px + 0px + 0rem)',
  },
  {
    index: 2,
    expected: 'calc(var(--chakra-space-3) + var(--chakra-sizes-12) + 12px + 48px + 1rem)',
  },
]

describe('getHandlePosition', () => {
  it.each<Suite>(validationSuite)('should get handle for index $index', ({ index, expected }) => {
    expect(getHandlePosition(index)).toStrictEqual(expected)
  })
})
