import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { handlers } from './__handlers__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={new QueryClient()}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetCapability', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should not load an non-existent capability', async () => {
    const { result } = renderHook(() => useGetCapability('wrong-capability'), { wrapper })

    await waitFor(() => {
      expect(result.current).toBeFalsy()
    })
  })

  it('should load a capability', async () => {
    const { result } = renderHook(() => useGetCapability('data-hub'), { wrapper })

    await waitFor(() => {
      expect(result.current).toBeTruthy()
    })
  })
})
