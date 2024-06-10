import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { handlers } from './__handlers__'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={new QueryClient()}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetConfiguration', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetConfiguration, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        cloudLink: expect.objectContaining({ displayText: 'HiveMQ Cloud' }),
        ctas: expect.objectContaining({}),
        documentationLink: expect.objectContaining({}),
        environment: expect.objectContaining({}),
        extensions: expect.objectContaining({}),
        firstUseInformation: expect.objectContaining({}),
        gitHubLink: expect.objectContaining({}),
        hivemqId: 'my-current-installation-id',
        modules: expect.objectContaining({}),
      })
    )
  })
})
