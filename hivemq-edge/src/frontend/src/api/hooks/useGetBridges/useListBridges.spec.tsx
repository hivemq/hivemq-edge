import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { handlers } from './__handlers__'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={new QueryClient()}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useListBridges', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useListBridges, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual([
      expect.objectContaining({
        cleanStart: true,
        clientId: 'my-client-id',
        host: 'my.h0st.org',
        id: 'bridge-id-01',
        keepAlive: 0,
      }),
    ])
  })
})
