import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import '@/config/i18n.config.ts'

import { useGetEvents } from '@/api/hooks/useEvents/useGetEvents.ts'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'

describe('useGetEvents', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetEvents, { wrapper })
    await waitFor(
      () => {
        expect(result.current.isLoading).toBeFalsy()
        expect(result.current.isSuccess).toBeTruthy()
      },
      { timeout: 25000 }
    )
    expect(result.current.data?.items).toHaveLength(200)
  })
})
