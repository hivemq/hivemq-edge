import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { mappingHandlers, MOCK_SOUTHBOUND_MAPPING } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import type { SouthboundMappingList } from '@/api/__generated__'
import { type FieldMapping } from '@/api/__generated__'

import { useSouthboundMappingManager } from '@/modules/Mappings/hooks/useSouthboundMappingManager.ts'

describe('useSouthboundMappingManager', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should do it', async () => {
    const { result } = renderHook(() => useSouthboundMappingManager('my-adapter'), { wrapper })
    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.data).toStrictEqual<SouthboundMappingList>({
      items: [
        expect.objectContaining({
          tagName: 'my/tag',
          topicFilter: 'my/filter',
          fieldMapping: expect.objectContaining<FieldMapping>({
            instructions: [
              {
                destination: 'lastName',
                source: 'dropped-property',
              },
            ],
          }),
        }),
      ],
    })
  })

  it('should do it properly', async () => {
    const { result } = renderHook(() => useSouthboundMappingManager('my-adapter'), { wrapper })

    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // TODO[TEST] Needs to test the effect of calling the functions
    act(() => {
      result.current.onUpdateCollection({ items: [MOCK_SOUTHBOUND_MAPPING] })
    })

    act(() => {
      result.current.onClose()
    })
  })
})
