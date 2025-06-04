import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { useListSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useListSouthboundMappings.ts'

describe('useListSouthboundMappings', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useListSouthboundMappings('my-adapter'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({
      items: [
        expect.objectContaining({
          topicFilter: 'my/filter',
          tagName: 'my/tag',
        }),
      ],
    })
  })
})
