import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { type DomainTagList } from '@/api/__generated__'
import { useListDomainTags } from '@/api/hooks/useDomainModel/useListDomainTags.ts'

import { handlers } from './__handlers__'

describe('useListDomainTags', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useListDomainTags(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<DomainTagList>({
      items: [
        {
          protocolId: 'modbus',
          tagDefinition: {
            endIdx: 1,
            startIdx: 0,
          },
          tagName: 'test/tag1',
        },
      ],
    })
  })
})
