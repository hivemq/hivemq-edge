import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useGetAdapterFieldMappings } from '@/api/hooks/useProtocolAdapters/useGetAdapterFieldMappings.ts'

import { mappingHandlers } from './__handlers__/mapping.mocks.ts'

describe('useGetAdapterFieldMappings', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetAdapterFieldMappings('my-adapter'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.items).toStrictEqual([
      expect.objectContaining({
        tag: 'my/tag',
        topicFilter: 'my/filter',
        metadata: {
          destination: expect.objectContaining({ description: 'A simple form example.' }),
          source: expect.objectContaining({ description: 'A simple form example.' }),
        },
      }),
    ])
  })
})
