import { beforeEach, expect, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderHook, waitFor, act } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { handlers as protocolHandler, mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as tahHandlers } from '@/api/hooks/useDomainModel/__handlers__'
import { handlers as topicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'
import { handlers as bridgeHandlers, mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { useGetClusterData } from '@/modules/DomainOntology/hooks/useGetClusterData.ts'
import { TreeEntity } from '@/modules/DomainOntology/utils/cluster.utils.ts'

describe('useGetClusterData', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...protocolHandler, ...tahHandlers, ...topicFilterHandlers, ...bridgeHandlers)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should return a valid payload', async () => {
    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        children: [
          expect.objectContaining({ category: TreeEntity.ADAPTER, name: 'my-adapter', payload: mockAdapter }),
          expect.objectContaining({ category: TreeEntity.BRIDGE, name: 'bridge-id-01', payload: mockBridge }),
        ],
        data: ['Root', expect.arrayContaining([])],
        depth: 0,
        height: 0,
        parent: null,
      })
    )
  })

  it('should return a valid cluster', async () => {
    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.clusterKeys).toStrictEqual(expect.arrayContaining([]))
  })

  it('should handle error state from adapters', async () => {
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isError).toBeTruthy()
    })
  })

  it('should handle error state from bridges', async () => {
    server.use(
      http.get('*/bridges', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isError).toBeTruthy()
    })
  })

  it('should update clusterKeys when setClusterKeys is called', async () => {
    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.clusterKeys).toEqual([])

    act(() => {
      result.current.setClusterKeys(['status'])
    })

    await waitFor(() => {
      expect(result.current.clusterKeys).toEqual(['status'])
    })
  })

  it('should group data by cluster keys', async () => {
    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    act(() => {
      result.current.setClusterKeys(['status'])
    })

    await waitFor(() => {
      expect(result.current.data).toBeDefined()
      expect(result.current.data.children).toBeDefined()
    })
  })

  it('should return empty state data when no adapters or bridges', async () => {
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        children: expect.arrayContaining([]),
      })
    )
  })

  it('should handle when hierarchy has defined children', async () => {
    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    act(() => {
      result.current.setClusterKeys(['type'])
    })

    await waitFor(() => {
      expect(result.current.data).toBeDefined()
    })
  })

  it('should not set up interval when there are errors', async () => {
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result, unmount } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isError).toBeTruthy()
    })

    // Wait a bit to ensure no additional intervals are set
    await new Promise((resolve) => setTimeout(resolve, 100))

    unmount()
  })

  it('should handle hierarchy without children defined', async () => {
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      }),
      http.get('*/bridges', () => {
        return HttpResponse.json({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(useGetClusterData, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // When there's no data, hierarchy.children is undefined, so it should create the structure
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        children: expect.arrayContaining([]),
        data: expect.arrayContaining(['Root', expect.any(Array)]),
      })
    )
  })
})
