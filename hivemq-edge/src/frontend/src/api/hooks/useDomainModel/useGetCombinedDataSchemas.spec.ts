import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import type { JsonNode } from '@/api/__generated__'

import { DataReferenceType, useGetCombinedDataSchemas } from './useGetCombinedDataSchemas'
import { mappingHandlers } from '../useProtocolAdapters/__handlers__/mapping.mocks'
import { handlers } from '../useTopicFilters/__handlers__'

const mockFFF = [
  {
    id: 'my-tag',
    adapterId: 'string',
    type: DataReferenceType.TAG,
  },
  {
    id: 'a/topic/+/filter',
    type: DataReferenceType.TOPIC_FILTER,
  },
]

describe('useGetCombinedDataSchemas', () => {
  beforeEach(() => {
    // server.use(...handlers)
    server.use(...mappingHandlers, ...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetCombinedDataSchemas(mockFFF), { wrapper })
    await waitFor(() => {
      expect(result.current).not.toBeUndefined()
      expect(result.current.every((e) => e.isLoading)).toBeFalsy()
    })
    expect(result.current[0].data).toStrictEqual<JsonNode>(
      expect.objectContaining({ description: 'A simple form example.', title: 'my-tag', type: 'object' })
    )

    expect(result.current[1].data).not.toBeUndefined()
  })
})
