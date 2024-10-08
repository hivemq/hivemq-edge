import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { useGetMetrics } from '@/api/hooks/useGetMetrics/useGetMetrics.ts'
import { handlers } from '@/api/hooks/useGetMetrics/__handlers__'

describe('useGetMetrics', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useGetMetrics, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.items).toStrictEqual(
      expect.arrayContaining([
        {
          name: 'com.hivemq.edge.bridge.bridge-id-01.forward.publish.count',
        },
        { name: 'com.hivemq.edge.bridge.bridge-id-01.remote.publish.loop-hops-exceeded.count' },
        { name: 'com.hivemq.edge.protocol-adapters.simulation.my-adapter.connection.success.count' },
      ])
    )
  })
})
