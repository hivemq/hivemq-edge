import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { MOCK_MAX_QOS } from '@/__test-utils__/adapters/mqtt.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { mappingHandlers, MOCK_NORTHBOUND_MAPPING } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import type { NorthboundMappingList } from '@/api/__generated__'

import { useNorthboundMappingManager } from '@/modules/Mappings/hooks/useNorthboundMappingManager.ts'

describe('useNorthboundMappingManager', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should do it', async () => {
    const { result } = renderHook(() => useNorthboundMappingManager('my-adapter'), { wrapper })

    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.data).toStrictEqual<NorthboundMappingList>({
      items: [
        expect.objectContaining({
          includeTagNames: true,
          includeTimestamp: true,
          maxQoS: MOCK_MAX_QOS,
          messageExpiryInterval: -1000,
          tagName: 'my/tag',
          topic: 'my/topic',
        }),
      ],
    })
  })

  it('should do it properly', async () => {
    const { result } = renderHook(() => useNorthboundMappingManager('my-adapter'), { wrapper })

    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // TODO[TEST] Needs to test the effect of calling the functions
    act(() => {
      result.current.onUpdateCollection({ items: [MOCK_NORTHBOUND_MAPPING] })
    })

    act(() => {
      result.current.onClose()
    })
  })
})
