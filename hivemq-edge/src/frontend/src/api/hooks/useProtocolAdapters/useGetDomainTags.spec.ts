import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { deviceHandlers } from './__handlers__'
import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.tsx'
import type { DomainTagList } from '@/api/__generated__'

describe('useGetDomainTags', () => {
  beforeEach(() => {
    server.use(...deviceHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetDomainTags('adapterId'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<DomainTagList>({
      items: [
        {
          dataPoint: {
            node: 'ns=3;i=1002',
          },
          tag: 'adapterId/power-management/alert',
        },
        {
          dataPoint: {
            node: 'ns=3;i=1003',
          },
          tag: 'adapterId/power-management/off',
        },
        {
          dataPoint: {
            node: 'ns=3;i=1008',
          },
          tag: 'adapterId/log/event',
        },
      ],
    })
  })
})
