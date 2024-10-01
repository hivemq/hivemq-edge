import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.ts'

describe('useGetUnifiedNamespace', () => {
  beforeEach(() => {
    server.use(...handlers)
  })
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useGetUnifiedNamespace, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        area: 'area',
        enabled: false,
        enterprise: 'enterprise',
        prefixAllTopics: true,
        productionLine: 'production-line',
        site: 'site',
        workCell: 'work-cell',
      })
    )
  })
})
