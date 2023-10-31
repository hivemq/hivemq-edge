import { expect } from 'vitest'
import { AriaAttributes } from 'react'
import { getAriaSort } from '@/components/PaginatedTable/utils/table-utils.ts'
import { SortDirection } from '@tanstack/react-table'

interface TestEachSuite {
  canSort: boolean
  isSorted: false | SortDirection
  aria: AriaAttributes['aria-sort']
}

describe('getAriaSort', () => {
  it.each<TestEachSuite>([
    { canSort: false, isSorted: false, aria: undefined },
    { canSort: false, isSorted: 'asc', aria: undefined },
    { canSort: false, isSorted: 'desc', aria: undefined },
    { canSort: true, isSorted: false, aria: 'none' },
    { canSort: true, isSorted: 'asc', aria: 'ascending' },
    { canSort: true, isSorted: 'desc', aria: 'descending' },
  ])('should return $aria with $canSort + $isSorted', ({ canSort, isSorted, aria }) => {
    expect(getAriaSort(canSort, isSorted)).toStrictEqual(aria)
  })
})
