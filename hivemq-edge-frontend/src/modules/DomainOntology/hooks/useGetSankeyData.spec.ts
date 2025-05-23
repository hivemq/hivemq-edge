import { beforeEach, expect, vi } from 'vitest'

import { renderHook, waitFor } from '@testing-library/react'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { handlers as protocolHandler } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as tahHandlers } from '@/api/hooks/useDomainModel/__handlers__'
import { handlers as topicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'
import { handlers as bridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import { useGetSankeyData } from '@/modules/DomainOntology/hooks/useGetSankeyData.ts'

describe('useGetSankeyData', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...protocolHandler, ...tahHandlers, ...topicFilterHandlers, ...bridgeHandlers)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should return a valid payload', async () => {
    const { result } = renderHook(useGetSankeyData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.sankeyData).toStrictEqual(
      expect.objectContaining({
        data: {
          links: expect.arrayContaining([
            {
              source: '#',
              target: 'prefix/{#}/bridge/${bridge.name}',
              value: 1,
            },
            {
              source: 'root/topic/ref/1',
              target: 'prefix/{#}/bridge/${bridge.name}',
              value: 1,
            },
          ]),
          nodes: expect.arrayContaining([
            {
              id: 'my/topic',
            },
            {
              id: 'test/tag1',
            },
            {
              id: 'a/topic/+/filter',
            },
            {
              id: 'prefix/{#}/bridge/${bridge.name}',
            },
            {
              id: '#',
            },
            {
              id: 'root/topic/ref/1',
            },
          ]),
        },
      })
    )
  })
})
