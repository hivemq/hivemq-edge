import { describe, expect } from 'vitest'

import { applyFacets } from './facets-utils.ts'
import { ProtocolFacetType } from '@/modules/ProtocolAdapters/types.ts'
import { ProtocolAdapter } from '@/api/__generated__'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

interface TestEachSuite {
  a: ProtocolFacetType
  b: ProtocolAdapter
  desc: string
  expected: boolean
}

describe('applyFacets', () => {
  test.each<TestEachSuite>([
    { a: {}, b: mockProtocolAdapter, desc: 'undefined facet', expected: true },

    { a: { filter: undefined }, b: mockProtocolAdapter, desc: 'undefined filter', expected: true },
    { a: { filter: { value: '', key: 'category' } }, b: mockProtocolAdapter, desc: 'empty value', expected: true },
    {
      a: { filter: { value: 'INDUSTRIAL', key: 'category' } },
      b: mockProtocolAdapter,
      desc: 'a proper category',
      expected: true,
    },
    { a: { filter: { value: 'tag1', key: 'tags' } }, b: mockProtocolAdapter, desc: 'a proper tag', expected: true },

    { a: { search: undefined }, b: mockProtocolAdapter, desc: 'undefined search term', expected: true },
    { a: { search: 'Edge Device' }, b: mockProtocolAdapter, desc: 'a term in the name', expected: true },
    { a: { search: 'from an edge' }, b: mockProtocolAdapter, desc: 'a term in the description', expected: true },
  ])('should returns TRUE with $desc', ({ a, b, expected }) => {
    expect(applyFacets(a)(b)).toBe(expected)
  })

  test.each<TestEachSuite>([
    {
      a: { filter: { value: 'wrong category', key: 'category' } },
      b: mockProtocolAdapter,
      desc: 'a wrong category',
      expected: false,
    },
    {
      a: { filter: { value: 'wrong tag', key: 'tags' } },
      b: mockProtocolAdapter,
      desc: 'a wrong tag',
      expected: false,
    },

    { a: { search: 'no match' }, b: mockProtocolAdapter, desc: 'no search match', expected: false },
  ])('should returns FALSE with $desc', ({ a, b, expected }) => {
    expect(applyFacets(a)(b)).toBe(expected)
  })
})
