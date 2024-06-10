import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { handlers } from './__handlers__'
import { useGetProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useGetProtocolAdapter.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={new QueryClient()}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetProtocolAdapter', () => {
  beforeEach(() => {
    server.use(...handlers)
  })
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetProtocolAdapter('test'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        id: 'test',
        status: {
          connection: 'CONNECTED',
          startedAt: '2023-08-21T11:51:24.234+01',
        },
        type: 'simulation',
        config: {
          id: 'my-adapter',
          pollingIntervalMillis: 10000,
          subscriptions: [
            {
              destination: 'root/topic/ref/1',
              qos: 0,
            },
            {
              destination: 'root/topic/ref/2',
              qos: 0,
            },
          ],
        },
      })
    )
  })
})
