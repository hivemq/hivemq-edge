import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, beforeEach, afterEach, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import queryClient from '@/api/queryClient.ts'

import '@/config/i18n.config.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { EdgeFlowProvider } from './FlowContext.tsx'
import useGetFlowElements from './useGetFlowElements.tsx'
import { QueryClientProvider } from '@tanstack/react-query'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { handlers } from '@/__test-utils__/msw/handlers.ts'
import { MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'
import { WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'

// [Vitest] Mocking hooks
vi.mock('@chakra-ui/react', async () => {
  const actual = await vi.importActual('@chakra-ui/react')

  // @ts-ignore
  return { ...actual, useTheme: vi.fn<[], Partial<WithCSSVar<Dict>>>(() => MOCK_THEME) }
})

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <MemoryRouter>
        <EdgeFlowProvider>{children}</EdgeFlowProvider>
      </MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetFlowElements', () => {
  beforeEach(() => {
    window.localStorage.clear()
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
    queryClient.clear()
  })

  it('should be used in the right context', async () => {
    const { result } = renderHook(() => useGetFlowElements(), { wrapper })

    // [Vitest] Need to make sure the async requests have been intercepted
    await waitFor(() => {
      const { nodes } = result.current
      expect(!!nodes.length).toBeTruthy()
    })

    expect(result.current.nodes).toHaveLength(3)
    expect(result.current.edges).toHaveLength(2)
  })

  it('should be used in the right context', async () => {
    const { result } = renderHook(() => useGetFlowElements(), { wrapper })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)
  })
})
