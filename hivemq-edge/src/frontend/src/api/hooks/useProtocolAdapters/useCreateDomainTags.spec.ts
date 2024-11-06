import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { deviceHandlers, MOCK_DEVICE_TAGS } from './__handlers__'
import { useCreateDomainTags } from '@/api/hooks/useProtocolAdapters/useCreateDomainTags.ts'

describe('useCreateDomainTags', () => {
  beforeEach(() => {
    server.use(...deviceHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useCreateDomainTags, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()

    act(() => {
      result.current.mutateAsync({ adapterId: 'my-adapter', requestBody: MOCK_DEVICE_TAGS('my-adapter')[0] })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({})
    })
  })
})
