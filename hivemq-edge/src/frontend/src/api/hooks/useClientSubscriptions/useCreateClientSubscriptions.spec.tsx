import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useCreateClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useCreateClientSubscriptions.ts'
import { mockClientSubscription } from '@/api/hooks/useClientSubscriptions/__handlers__'

describe('useListProtocolAdapters', () => {
  it('should load the data', async () => {
    const { result } = renderHook(() => useCreateClientSubscriptions(), { wrapper })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeFalsy()
    })

    act(() => {
      result.current.mutateAsync({ id: 'fake-id', requestBody: mockClientSubscription.config })
    })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual(
        expect.objectContaining({
          id: 'fake-id',
          requestBody: mockClientSubscription.config,
        })
      )
    })
  })
})
