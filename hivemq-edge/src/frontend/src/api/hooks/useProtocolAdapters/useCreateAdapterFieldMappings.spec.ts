import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useCreateAdapterFieldMappings } from '@/api/hooks/useProtocolAdapters/useCreateAdapterFieldMappings.ts'
import { mappingHandlers, MOCK_MAPPING } from './__handlers__/mapping.mocks.ts'

describe('useCreateAdapterFieldMappings', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...mappingHandlers)

    const { result } = renderHook(() => useCreateAdapterFieldMappings(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutate({
        adapterId: 'my-adapter',
        requestBody: MOCK_MAPPING,
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        adapterId: 'my-adapter',
        tag: 'my/tag',
        topicFilter: 'my/filter',
      })
    })
  })
})
