import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useGetDomainTag } from '@/api/hooks/useDomainModel/useGetDomainTag.ts'
import { DomainTag } from '@/api/__generated__'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { MOCK_DEVICE_TAG_FAKE } from '@/api/hooks/useProtocolAdapters/__handlers__'

import { handlers } from './__handlers__'

describe('useGetDomainTag', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const mockTag = 'tag/test'
    const { result } = renderHook(() => useGetDomainTag(mockTag), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<DomainTag>({
      definition: {
        endIdx: 1,
        startIdx: 0,
      },
      name: mockTag,
    })
  })

  it('should return an error', async () => {
    const { result } = renderHook(() => useGetDomainTag(MOCK_DEVICE_TAG_FAKE), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current.data).toStrictEqual(undefined)
    expect(result.current.error?.status).toStrictEqual(404)
    expect(result.current.error?.body).toStrictEqual<ProblemDetails>({ title: 'The tag is not found', status: 404 })
  })
})
