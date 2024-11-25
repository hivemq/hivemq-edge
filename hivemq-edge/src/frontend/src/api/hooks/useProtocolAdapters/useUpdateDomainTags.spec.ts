import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { deviceHandlers, MOCK_DEVICE_TAGS } from './__handlers__'
import { useUpdateDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateDomainTags.ts'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

describe('useUpdateDomainTags', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...deviceHandlers)

    const { result } = renderHook(useUpdateDomainTags, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({
        adapterId: 'my-adapter',
        tagId: encodeURIComponent('test/segment2/topic1'),
        requestBody: MOCK_DEVICE_TAGS('my-adapter', MockAdapterType.SIMULATION)[0],
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({})
    })
  })
})
