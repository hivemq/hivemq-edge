import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { type JsonNode } from '@/api/__generated__'
import { useGetSchemaForTopic } from '@/api/hooks/useDomainModel/useGetSchemaForTopic.ts'

import { handlers } from './__handlers__'

describe('useGetSchemaForTopic', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetSchemaForTopic('test/topic1', true), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<JsonNode>(
      expect.objectContaining({
        title: 'dGVzdC90b3BpYzE=',
        type: 'object',
      })
    )
  })

  it('should not load if not seeded', async () => {
    const { result } = renderHook(() => useGetSchemaForTopic('test/topic1', false), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isFetched).toBeFalsy()
    })
    expect(result.current.data).toBeUndefined()
    expect(result.current.error).toBeNull()
  })
})
