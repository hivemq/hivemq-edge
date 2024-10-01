import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import type { TagSchema } from '@/api/__generated__'
import { handlers } from './__handlers__'
import { useGetTopicSchemas } from '@/api/hooks/useDomainModel/useGetTopicSchemas.ts'

describe('useGetTopicSchemas', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)
    const { result } = renderHook(() => useGetTopicSchemas(['test']), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<TagSchema>([])
  })
})
