import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, beforeEach, afterEach, vi } from 'vitest'
import queryClient from '@/api/queryClient.ts'

import '@/config/i18n.config.ts'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { handlers as useFrontendServices } from '@/api/hooks/useFrontendServices/__handlers__'
import { handlers as ProtocolAdapterHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as DataHubDataPoliciesService } from '@/extensions/datahub/api/hooks/DataHubDataPoliciesService/__handlers__'

import { useGetPoliciesMatching } from '@/modules/Workspace/hooks/useGetPoliciesMatching.ts'
import {
  MOCK_NODE_ADAPTER,
  MOCK_NODE_BRIDGE,
  MOCK_NODE_GROUP,
  MOCK_NODE_LISTENER,
} from '@/__test-utils__/react-flow/nodes.ts'

vi.mock('@/modules/Workspace/hooks/useWorkspaceStore.ts', async () => {
  return { default: () => ({ nodes: [MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE, MOCK_NODE_GROUP, MOCK_NODE_LISTENER] }) }
})

describe('useGetPoliciesMatching', () => {
  beforeEach(() => {
    server.use(...useFrontendServices, ...DataHubDataPoliciesService, ...ProtocolAdapterHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
    queryClient.clear()
  })

  it('should not find anything', async () => {
    const { result } = renderHook(() => useGetPoliciesMatching('not in'), { wrapper })

    await waitFor(() => {
      const policies = result.current
      expect(policies).not.toBeUndefined()
    })
    expect(result.current).toEqual([])
  })

  it('should find policies for an adapter', async () => {
    const { result } = renderHook(() => useGetPoliciesMatching('idAdapter'), { wrapper })

    await waitFor(() => {
      const policies = result.current
      expect(policies).not.toBeUndefined()
    })
    expect(result.current).toEqual([
      {
        createdAt: '2023-10-13T11:51:24.234Z',
        id: 'my-policy-id',
        matching: {
          topicFilter: 'root/topic/ref/1',
        },
      },
    ])
  })

  it('should find policies for a bridge', async () => {
    const { result } = renderHook(() => useGetPoliciesMatching('idBridge'), { wrapper })

    await waitFor(() => {
      const policies = result.current
      expect(policies).not.toBeUndefined()
    })
    expect(result.current).toEqual([
      {
        createdAt: '2023-10-13T11:51:24.234Z',
        id: 'my-policy-id',
        matching: {
          topicFilter: 'root/topic/ref/1',
        },
      },
    ])
  })

  it('should find policies for a group', async () => {
    const { result } = renderHook(() => useGetPoliciesMatching('idGroup'), { wrapper })

    await waitFor(() => {
      const policies = result.current
      expect(policies).not.toBeUndefined()
    })
    expect(result.current).toEqual([
      {
        createdAt: '2023-10-13T11:51:24.234Z',
        id: 'my-policy-id',
        matching: {
          topicFilter: 'root/topic/ref/1',
        },
      },
    ])
  })

  it('should not find any policies for the other nodes', async () => {
    const { result } = renderHook(() => useGetPoliciesMatching('idListener'), { wrapper })

    await waitFor(() => {
      const policies = result.current
      expect(policies).not.toBeUndefined()
    })
    expect(result.current).toEqual([])
  })
})
