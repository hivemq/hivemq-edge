import { beforeEach, expect, vi } from 'vitest'
import { HierarchyNode } from 'd3-hierarchy'

import { renderHook, waitFor } from '@testing-library/react'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { handlers as protocolHandler } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as tahHandlers } from '@/api/hooks/useDomainModel/__handlers__'
import { handlers as topicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'
import { handlers as bridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import { useGetSunburstData } from '@/modules/DomainOntology/hooks/useGetSunburstData.ts'

describe('useGetSunburstData', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...protocolHandler, ...tahHandlers, ...topicFilterHandlers, ...bridgeHandlers)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should return a valid payload', async () => {
    const { result } = renderHook(useGetSunburstData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.sunburstData).toStrictEqual(
      expect.objectContaining<Partial<HierarchyNode<{ label: string } | null>>>({
        children: expect.arrayContaining([]),
        data: null,
        depth: 0,
        height: 4,
        id: '/',
        parent: null,
      })
    )
  })
})
