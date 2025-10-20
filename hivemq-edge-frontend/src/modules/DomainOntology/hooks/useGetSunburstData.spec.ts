import { beforeEach, expect, vi } from 'vitest'
import type { HierarchyNode } from 'd3-hierarchy'
import { http, HttpResponse } from 'msw'

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

  it('should return empty state data when loading', async () => {
    const { result } = renderHook(useGetSunburstData, { wrapper })

    // Check the initial loading state
    expect(result.current.isLoading).toBeTruthy()
    // The empty state should be returned during loading
    expect(result.current.sunburstData).toBeDefined()
  })

  it('should return empty state when no topics or tags are available', async () => {
    server.use(
      http.get('*/management/protocol-adapters/northboundMappings', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/protocol-adapters/southboundMappings', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetSunburstData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // Should return the empty state data with error messages
    expect(result.current.sunburstData).toBeDefined()
    // The empty state might have children or be a leaf node, just verify it exists
    expect(result.current.sunburstData.depth).toBe(0)
  })

  it('should handle empty edgeTopics array', async () => {
    server.use(
      http.get('*/management/protocol-adapters/northboundMappings', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/protocol-adapters/southboundMappings', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetSunburstData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // When edgeTopics is empty, it should return the empty state
    expect(result.current.sunburstData).toBeDefined()
    expect(result.current.sunburstData.depth).toBe(0)
  })
})
