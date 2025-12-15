import { beforeEach, expect, vi } from 'vitest'
import { http, HttpResponse } from 'msw'

import { renderHook, waitFor } from '@testing-library/react'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import type { NorthboundMappingOwnerList, SouthboundMappingOwnerList } from '@/api/__generated__'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { handlers as protocolHandler } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as tahHandlers } from '@/api/hooks/useDomainModel/__handlers__'
import { handlers as topicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'
import { handlers as bridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import { useGetTreeData } from '@/modules/DomainOntology/hooks/useGetTreeData.ts'
import type { Tree, TreeLeaf, TreeNode } from '@/modules/DomainOntology/types.ts'

describe('useGetTreeData', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...protocolHandler, ...tahHandlers, ...topicFilterHandlers, ...bridgeHandlers)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should return a valid payload', async () => {
    const { result } = renderHook(useGetTreeData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.treeData).toStrictEqual(
      expect.objectContaining<Tree>({
        type: 'node',
        name: 'Edge',
        value: 0,
        children: expect.arrayContaining([]),
      })
    )
  })

  it('should handle bridge topic filters and topics', async () => {
    server.use(
      http.get('*/bridges', () => {
        return HttpResponse.json(
          {
            items: [
              {
                id: 'test-bridge',
                localSubscriptions: [{ filters: ['bridge/filter/+'] }],
                remoteSubscriptions: [{ filters: ['bridge/topic/#'] }],
              },
            ],
          },
          { status: 200 }
        )
      }),
      http.get('*/management/protocol-adapters/mappings/northboundMappings', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetTreeData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // Type guard to check if treeData is TreeNode
    expect(result.current.treeData.type).toBe('node')
    const treeData = result.current.treeData as TreeNode

    expect(treeData.children).toBeDefined()
    const topicFiltersNode = treeData.children?.find((child: Tree) => child.name === 'Topic Filters') as TreeNode
    expect(topicFiltersNode).toBeDefined()
    expect(topicFiltersNode?.children).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: 'bridge/filter/+', type: 'leaf' }),
        expect.objectContaining({ name: 'bridge/topic/#', type: 'leaf' }),
      ])
    )
  })

  it('should build links for north mappings', async () => {
    // Override with explicit mock data to ensure tags and northbound mappings match
    server.use(
      http.get('*/management/protocol-adapters/mappings/northboundMappings', () => {
        return HttpResponse.json<NorthboundMappingOwnerList>(
          {
            items: [{ adapterId: 'test-adapter', tagName: 'test/tag1', topic: 'my/topic' }],
          },
          { status: 200 }
        )
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json(
          {
            items: [{ name: 'test/tag1' }],
          },
          { status: 200 }
        )
      }),
      http.get('*/management/protocol-adapters/mappings/southboundMappings', () => {
        return HttpResponse.json<SouthboundMappingOwnerList>({ items: [] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetTreeData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // Type guard to check if treeData is TreeNode
    expect(result.current.treeData.type).toBe('node')
    const treeData = result.current.treeData as TreeNode

    const tagsNode = treeData.children?.find((child: Tree) => child.name === 'Tags') as TreeNode
    expect(tagsNode).toBeDefined()
    expect(tagsNode?.children).toBeDefined()

    // Verify that at least some tags have links built from north mappings
    const allTags = (tagsNode?.children || []) as TreeLeaf[]
    const tagsWithLinks = allTags.filter((tag) => tag.links && tag.links.length > 0)

    // At least one tag should have links from the north mappings (covering lines 52-54)
    expect(tagsWithLinks.length).toBeGreaterThan(0)

    // Verify the specific tag has the expected link
    const testTag = allTags.find((tag) => tag.name === 'test/tag1')
    expect(testTag).toBeDefined()
    expect(testTag?.links).toContain('my/topic')
  })

  it('should build links for south mappings', async () => {
    server.use(
      http.get('*/management/protocol-adapters/mappings/southboundMappings', () => {
        return HttpResponse.json<SouthboundMappingOwnerList>(
          {
            items: [
              { adapterId: 'test-adapter', topicFilter: 'input/filter1', tagName: 'device/tag1' },
              { adapterId: 'test-adapter', topicFilter: 'input/filter2', tagName: 'device/tag2' },
            ],
          },
          { status: 200 }
        )
      }),
      http.get('*/management/protocol-adapters/mappings/northboundMappings', () => {
        return HttpResponse.json<NorthboundMappingOwnerList>({ items: [] }, { status: 200 })
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json(
          {
            items: [{ topicFilter: 'input/filter1' }, { topicFilter: 'input/filter2' }],
          },
          { status: 200 }
        )
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetTreeData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // Type guard to check if treeData is TreeNode
    expect(result.current.treeData.type).toBe('node')
    const treeData = result.current.treeData as TreeNode

    const filtersNode = treeData.children?.find((child: Tree) => child.name === 'Topic Filters') as TreeNode
    expect(filtersNode).toBeDefined()
    const filter1 = filtersNode?.children?.find((child: Tree) => child.name === 'input/filter1') as TreeLeaf
    const filter2 = filtersNode?.children?.find((child: Tree) => child.name === 'input/filter2') as TreeLeaf

    expect(filter1?.links).toContain('device/tag1')
    expect(filter2?.links).toContain('device/tag2')
  })

  it('should build links for topic filter matching', async () => {
    server.use(
      http.get('*/management/protocol-adapters/mappings/northboundMappings', () => {
        return HttpResponse.json<NorthboundMappingOwnerList>(
          {
            items: [
              { adapterId: 'test-adapter', tagName: 'test/tag', topic: 'sensor/temperature/room1' },
              { adapterId: 'test-adapter', tagName: 'test/tag', topic: 'sensor/humidity/room2' },
            ],
          },
          { status: 200 }
        )
      }),
      http.get('*/management/domain-tags', () => {
        return HttpResponse.json({ items: [{ name: 'test/tag' }] }, { status: 200 })
      }),
      http.get('*/management/topic-filters', () => {
        return HttpResponse.json(
          {
            items: [{ topicFilter: 'sensor/+/room1' }, { topicFilter: 'sensor/#' }],
          },
          { status: 200 }
        )
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetTreeData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // Type guard to check if treeData is TreeNode
    expect(result.current.treeData.type).toBe('node')
    const treeData = result.current.treeData as TreeNode

    const topicsNode = treeData.children?.find((child: Tree) => child.name === 'Topics') as TreeNode
    expect(topicsNode).toBeDefined()

    const topic1 = topicsNode?.children?.find((child: Tree) => child.name === 'sensor/temperature/room1') as TreeLeaf
    const topic2 = topicsNode?.children?.find((child: Tree) => child.name === 'sensor/humidity/room2') as TreeLeaf

    // topic1 should match both filters
    expect(topic1?.links).toContain('sensor/+/room1')
    expect(topic1?.links).toContain('sensor/#')

    // topic2 should only match the wildcard filter
    expect(topic2?.links).toContain('sensor/#')
  })
})
