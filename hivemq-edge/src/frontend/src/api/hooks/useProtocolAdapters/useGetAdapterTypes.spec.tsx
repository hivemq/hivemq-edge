import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'

describe('useGetAdapterTypes', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useGetAdapterTypes, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.items).toStrictEqual([
      expect.objectContaining({
        id: 'simulation',
        installed: true,
        logoUrl: 'http://localhost:8080/images/hivemq-icon.png',
        name: 'Simulated Edge Device',
        protocol: 'Simulation',
        tags: ['tag1', 'tag2', 'tag3'],
        url: 'https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#simulation-adapter',
        version: 'Development Snapshot',
      }),
    ])
  })
})
