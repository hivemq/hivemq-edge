import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import type { TagSchema } from '@/api/__generated__'
import { handlers } from './__handlers__'
import { useGetTagSchemas } from '@/api/hooks/useDomainModel/useGetTagSchemas.ts'

describe('useGetTagSchemas', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)
    const { result } = renderHook(() => useGetTagSchemas(['test']), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<TagSchema>([])
  })
})
