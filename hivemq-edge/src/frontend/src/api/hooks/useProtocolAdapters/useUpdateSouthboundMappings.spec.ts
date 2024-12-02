import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { mappingHandlers, MOCK_SOUTHBOUND_MAPPING } from './__handlers__/mapping.mocks.ts'
import { useUpdateSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useUpdateSouthboundMappings.ts'

describe('useUpdateSouthboundMappings', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...mappingHandlers)

    const { result } = renderHook(() => useUpdateSouthboundMappings(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutate({
        adapterId: 'my-adapter',
        requestBody: { items: [MOCK_SOUTHBOUND_MAPPING] },
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        adapterId: 'my-adapter',
        items: [
          {
            tagName: 'my/tag',
            topicFilter: 'my/filter',
          },
        ],
      })
    })
  })
})
