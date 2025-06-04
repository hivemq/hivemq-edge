import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

import { type TagSchema } from '@/api/__generated__'
import { useGetDomainTagSchema } from '@/api/hooks/useDomainModel/useGetDomainTagSchema.ts'

import { handlers } from './__handlers__'

describe('useGetDomainTagSchema', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetDomainTagSchema(MockAdapterType.OPC_UA), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<TagSchema>({
      protocolId: 'opcua',
      configSchema: expect.objectContaining({
        $schema: 'https://json-schema.org/draft/2020-12/schema',
      }),
    })
  })

  it('should not load if protocolId is not given', async () => {
    const { result } = renderHook(() => useGetDomainTagSchema(undefined), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isFetched).toBeFalsy()
    })
    expect(result.current.data).toBeUndefined()
    expect(result.current.error).toBeNull()
  })
})
