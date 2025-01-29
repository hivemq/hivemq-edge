import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, beforeEach, afterEach, vi } from 'vitest'
import type { WithCSSVar } from '@chakra-ui/react'
import type { Dict } from '@chakra-ui/utils'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'
import { getWrapperEdgeProvider } from '@/__test-utils__/hooks/WrapperEdgeProvider.tsx'
import queryClient from '@/api/queryClient.ts'

import '@/config/i18n.config.ts'

import useGetFlowElements from './useGetFlowElements.ts'
import type { EdgeFlowOptions } from '@/modules/Workspace/types.ts'
import { handlers as BridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import { handlers as ProtocolAdapterHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as ListenerHandlers } from '@/api/hooks/useGateway/__handlers__'

// [Vitest] Mocking hooks
vi.mock('@chakra-ui/react', async () => {
  const actual = await vi.importActual('@chakra-ui/react')

  // @ts-ignore
  return { ...actual, useTheme: vi.fn<[], Partial<WithCSSVar<Dict>>>(() => MOCK_THEME) }
})

describe('useGetFlowElements', () => {
  beforeEach(() => {
    // window.localStorage.clear()
    server.use(...BridgeHandlers, ...ProtocolAdapterHandlers, ...ListenerHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
    queryClient.clear()
  })

  it.each<[Partial<EdgeFlowOptions>, number, number]>([
    [{}, 5, 4],
    [{ showGateway: true }, 6, 5],
    [{ showGateway: false }, 5, 4],
  ])('should consider %s for %s nodes and %s edges', async (defaults, countNode, countEdge) => {
    const { result } = renderHook(() => useGetFlowElements(), { wrapper: getWrapperEdgeProvider(defaults) })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.nodes).toHaveLength(countNode)
    expect(result.current.edges).toHaveLength(countEdge)
  })
})
