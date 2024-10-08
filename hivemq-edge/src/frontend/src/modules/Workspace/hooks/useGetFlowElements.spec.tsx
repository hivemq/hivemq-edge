import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, beforeEach, afterEach, vi } from 'vitest'
import { WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { handlers } from '@/__test-utils__/msw/handlers.ts'
import { MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'
import { SimpleWrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import queryClient from '@/api/queryClient.ts'

import '@/config/i18n.config.ts'

import { EdgeFlowProvider } from './FlowContext.tsx'
import useGetFlowElements from './useGetFlowElements.ts'
import { EdgeFlowOptions } from '@/modules/Workspace/types.ts'
import { handlers as ClientHandlers } from '@/api/hooks/useClientSubscriptions/__handlers__'

// [Vitest] Mocking hooks
vi.mock('@chakra-ui/react', async () => {
  const actual = await vi.importActual('@chakra-ui/react')

  // @ts-ignore
  return { ...actual, useTheme: vi.fn<[], Partial<WithCSSVar<Dict>>>(() => MOCK_THEME) }
})

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <SimpleWrapper>
    <EdgeFlowProvider>{children}</EdgeFlowProvider>
  </SimpleWrapper>
)

describe('useGetFlowElements', () => {
  beforeEach(() => {
    window.localStorage.clear()
    server.use(...handlers, ...ClientHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
    queryClient.clear()
  })

  it('should be used in the right context', async () => {
    const { result } = renderHook(() => useGetFlowElements(), { wrapper })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)
  })

  it.each<[Partial<EdgeFlowOptions>, number, number]>([
    [{}, 1, 0],
    [{ showGateway: true }, 1, 0],
    [{ showGateway: false }, 1, 0],
  ])('should consider %s for %s nodes and %s edges', async (defaults, countNode, countEdge) => {
    const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
      <SimpleWrapper>
        <EdgeFlowProvider defaults={defaults}>{children}</EdgeFlowProvider>
      </SimpleWrapper>
    )

    const { result } = renderHook(() => useGetFlowElements(), { wrapper })

    await waitFor(() => {
      const { nodes } = result.current
      expect(!!nodes.length).toBeTruthy()
    })

    expect(result.current.nodes).toHaveLength(countNode)
    expect(result.current.edges).toHaveLength(countEdge)
  })
})
