import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useListClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useListClientSubscriptions.ts'

describe('useListProtocolAdapters', () => {
  it('should load the data', async () => {
    const { result } = renderHook(() => useListClientSubscriptions(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual([
      expect.objectContaining({
        config: {
          id: 'my-first-client',
          subscriptions: [
            {
              destination: 'test/topic/1',
              maxQoS: 0,
            },
          ],
        },
        id: 'my-first-client',
        type: 'broker-client',
      }),
      expect.objectContaining({
        config: {
          id: 'my-first-client2',
        },
        id: 'my-first-client2',
        type: 'broker-client',
      }),
    ])
  })
})
