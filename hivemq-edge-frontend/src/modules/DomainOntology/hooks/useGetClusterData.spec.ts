import { beforeEach, expect, vi } from 'vitest'

import { renderHook, waitFor } from '@testing-library/react'
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
})
