import { beforeEach, expect, vi } from 'vitest'

import { renderHook, waitFor } from '@testing-library/react'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { handlers as protocolHandler } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as tahHandlers } from '@/api/hooks/useDomainModel/__handlers__'
import { handlers as topicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'
import { handlers as bridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import type { ChordMatrixData } from '@/modules/DomainOntology/types.ts'
import { useGetChordMatrixData } from '@/modules/DomainOntology/hooks/useGetChordMatrixData.ts'

describe('useGetChordMatrixData', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...protocolHandler, ...tahHandlers, ...topicFilterHandlers, ...bridgeHandlers)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should return a valid payload', async () => {
    const { result } = renderHook(useGetChordMatrixData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.matrixData).toStrictEqual(
      expect.objectContaining<ChordMatrixData>({
        keys: [
          'a/topic/+/filter',
          'test/tag1',
          'my/topic',
          'prefix/{#}/bridge/${bridge.name}',
          '#',
          'root/topic/ref/1',
        ],
        matrix: [
          [0, 0, 0, 0, 0, 0],
          [0, 0, 0, 0, 0, 0],
          [0, 0, 0, 0, 0, 0],
          [0, 0, 0, 0, 3, 3],
          [0, 0, 0, 1, 0, 0],
          [0, 0, 0, 1, 0, 0],
        ],
      })
    )
  })
})
