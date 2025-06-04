import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { type NorthboundMappingList } from '@/api/__generated__'

import { useListDomainNorthboundMappings } from '@/api/hooks/useDomainModel/useListDomainNorthboundMappings.ts'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'

describe('useListDomainNorthboundMappings', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useListDomainNorthboundMappings(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<NorthboundMappingList>({
      items: [
        expect.objectContaining({
          includeTagNames: true,
          includeTimestamp: true,
          maxQoS: 'AT_MOST_ONCE',
          messageExpiryInterval: -1000,
          tagName: 'my/tag',
          topic: 'my/topic',
        }),
      ],
    })
  })
})
