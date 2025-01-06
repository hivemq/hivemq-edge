import { expect } from 'vitest'

import { formatItemName } from '@/components/rjsf/utils/array-items.utils.ts'

describe('formatItemName', () => {
  it.each([
    {
      stub: undefined,
      index: 1,
      content: undefined,
      result: 'item #1',
    },
    {
      stub: 'item',
      index: 1,
      content: undefined,
      result: 'item #1',
    },
    {
      stub: 'item',
      index: 1,
      content: 'the full name',
      result: 'item #1 - the full name',
    },
  ])('should return $value for $path', ({ stub, index, content, result }) => {
    expect(formatItemName(stub, index, content)).toStrictEqual(result)
  })
})
