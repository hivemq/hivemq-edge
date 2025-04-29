import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { MOCK_MAX_QOS } from '@/__test-utils__/adapters/mqtt.ts'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { useListNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useListNorthboundMappings.ts'

describe('useListNorthboundMappings', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useListNorthboundMappings('my-adapter'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({
      items: [
        expect.objectContaining({
          tagName: 'my/tag',
          topic: 'my/topic',
          includeTagNames: true,
          includeTimestamp: true,
          maxQoS: MOCK_MAX_QOS,
          messageExpiryInterval: -1000,
        }),
      ],
    })
  })
})
