import { beforeEach, expect, vi } from 'vitest'
import { http, HttpResponse } from 'msw'

import { renderHook, waitFor } from '@testing-library/react'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import type {
  NorthboundMappingOwnerList,
  SouthboundMappingOwner,
  SouthboundMappingOwnerList,
} from '@/api/__generated__'
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

  it('should handle southbound mappings with valid indices', async () => {
    server.use(
      http.get('*/management/protocol-adapters/mappings/southboundMappings', () => {
        return HttpResponse.json<SouthboundMappingOwnerList>(
          {
            items: [
              {
                adapterId: 'test-adapter',
                topicFilter: 'sensor/+/data',
                tagName: 'test/tag1',
              },
            ],
          },
          { status: 200 }
        )
      }),
      http.get('*/management/protocol-adapters/mappings/northboundMappings', () => {
        return HttpResponse.json<NorthboundMappingOwnerList>({ items: [] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json({ items: [{ topicFilter: 'sensor/+/data' }] }, { status: 200 })
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json({ items: [{ name: 'test/tag1' }] }, { status: 200 })
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetSankeyData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.sankeyData.data.links).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          source: 'sensor/+/data',
          target: 'test/tag1',
          value: 1,
        }),
      ])
    )
  })

  it('should skip southbound mappings with missing tagName', async () => {
    server.use(
      http.get('*/management/protocol-adapters/mappings/southboundMappings', () => {
        return HttpResponse.json<SouthboundMappingOwnerList>(
          {
            items: [
              {
                adapterId: 'test-adapter',
                topicFilter: 'sensor/+/data',
                tagName: null,
              } as unknown as SouthboundMappingOwner,
              { adapterId: 'test-adapter', topicFilter: 'device/#' } as unknown as SouthboundMappingOwner,
            ],
          },
          { status: 200 }
        )
      }),
      http.get('*/management/protocol-adapters/mappings/northboundMappings', () => {
        return HttpResponse.json<NorthboundMappingOwnerList>({ items: [] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetSankeyData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // Links should be empty because tagName is missing and items aren't in the arrays
    const southboundLinks = result.current.sankeyData.data.links.filter(
      (link) => link.source === 'sensor/+/data' || link.source === 'device/#'
    )
    expect(southboundLinks).toHaveLength(0)
  })
})
