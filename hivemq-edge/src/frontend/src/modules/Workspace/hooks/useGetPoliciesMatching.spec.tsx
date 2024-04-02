import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, beforeEach, afterEach, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import queryClient from '@/api/queryClient.ts'

import '@/config/i18n.config.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { QueryClientProvider } from '@tanstack/react-query'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { handlers } from '@/__test-utils__/msw/handlers.ts'

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

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetPoliciesMatching', () => {
  beforeEach(() => {
    server.use(...handlers)
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
        createdAt: '2023-10-13T11:51:24.234+01:00',
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
        createdAt: '2023-10-13T11:51:24.234+01:00',
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
        createdAt: '2023-10-13T11:51:24.234+01:00',
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
      expect(policies).toBeUndefined()
    })
  })
})
