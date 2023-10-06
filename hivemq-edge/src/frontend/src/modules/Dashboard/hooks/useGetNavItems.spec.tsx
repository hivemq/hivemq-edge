import { expect, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'

import { MemoryRouter } from 'react-router-dom'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import queryClient from '@/api/queryClient.ts'
import { handlers } from '@/api/hooks/useFrontendServices/__handlers__'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import '@/config/i18n.config.ts'

import useGetNavItems from './useGetNavItems.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useSpringClient', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should retrieve all the menu items', async () => {
    server.use(...handlers)
    const { result } = renderHook(useGetNavItems, { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data.map((e) => e.title)).toStrictEqual(['HiveMQ Edge', 'Extensions', 'External resources'])
    expect(result.current.data[0].items).toHaveLength(5)
    expect(result.current.data[2].items.map((e) => e.href)).toStrictEqual([
      'https://www.hivemq.com/articles/power-of-iot-data-management-in-smart-manufacturing/',
      'https://github.com/hivemq/hivemq-edge',
      'https://github.com/hivemq/hivemq-edge/wiki',
    ])
  })
})
