import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useGetWritingSchema } from '@/api/hooks/useProtocolAdapters/useGetWritingSchema.ts'

import type { JsonNode } from '@/api/__generated__'
import { mappingHandlers } from './__handlers__/mapping.mocks.ts'

describe('useGetWritingSchema', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetWritingSchema('my-adapter', 'my-tag'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<JsonNode>(
      expect.objectContaining<JsonNode>({
        description: 'A simple form example.',
        title: encodeURIComponent('my-tag'),
        properties: expect.objectContaining({
          age: expect.objectContaining({}),
          array: expect.objectContaining({}),
          firstName: expect.objectContaining({}),
        }),
      })
    )
  })
})
