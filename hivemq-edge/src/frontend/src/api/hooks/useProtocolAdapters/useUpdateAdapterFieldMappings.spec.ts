import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useUpdateAdapterFieldMappings } from '@/api/hooks/useProtocolAdapters/useUpdateAdapterFieldMappings.ts'
import { mappingHandlers, MOCK_MAPPING } from './__handlers__/mapping.mocks.ts'

describe('useUpdateAdapterFieldMappings', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...mappingHandlers)

    const { result } = renderHook(() => useUpdateAdapterFieldMappings(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutate({
        adapterId: 'my-adapter',
        requestBody: { items: [MOCK_MAPPING] },
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        adapterId: 'my-adapter',
        items: [
          {
            tag: 'my/tag',
            topicFilter: 'my/filter',
          },
        ],
      })
    })
  })
})
