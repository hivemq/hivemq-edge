import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { handlers } from './__handlers__'
import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={new QueryClient()}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetUnifiedNamespace', () => {
  beforeEach(() => {
    server.use(...handlers)
  })
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useGetUnifiedNamespace, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        area: 'area',
        enabled: false,
        enterprise: 'enterprise',
        prefixAllTopics: true,
        productionLine: 'production-line',
        site: 'site',
        workCell: 'work-cell',
      })
    )
  })
})
