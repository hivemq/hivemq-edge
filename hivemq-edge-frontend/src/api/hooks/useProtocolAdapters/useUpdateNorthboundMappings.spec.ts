import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { mappingHandlers, MOCK_NORTHBOUND_MAPPING } from './__handlers__/mapping.mocks.ts'
import { useUpdateNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useUpdateNorthboundMappings.ts'

describe('useUpdateNorthboundMappings', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...mappingHandlers)

    const { result } = renderHook(() => useUpdateNorthboundMappings(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutate({
        adapterId: 'my-adapter',
        requestBody: { items: [MOCK_NORTHBOUND_MAPPING] },
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        adapterId: 'my-adapter',
        items: [
          {
            tagName: 'my/tag',
            topic: 'my/topic',
          },
        ],
      })
    })
  })
})
