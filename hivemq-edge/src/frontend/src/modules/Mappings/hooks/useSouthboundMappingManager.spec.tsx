import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useSouthboundMappingManager } from '@/modules/Mappings/hooks/useSouthboundMappingManager.ts'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { type FieldMapping, SouthboundMappingList } from '@/api/__generated__'

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
            metadata: {
              destination: expect.objectContaining({}),
              source: expect.objectContaining({}),
            },
          }),
        }),
      ],
    })
  })
})
