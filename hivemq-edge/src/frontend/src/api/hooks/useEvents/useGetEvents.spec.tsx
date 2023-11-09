import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

import '@/config/i18n.config.ts'

import { useGetEvents } from '@/api/hooks/useEvents/useGetEvents.tsx'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { handlers } from './__handlers__'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={new QueryClient()}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetEvents', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetEvents, { wrapper })
    await waitFor(
      () => {
        expect(result.current.isLoading).toBeFalsy()
        expect(result.current.isSuccess).toBeTruthy()
      },
      { timeout: 25000 }
    )
    expect(result.current.data?.items).toHaveLength(200)
  })
})
