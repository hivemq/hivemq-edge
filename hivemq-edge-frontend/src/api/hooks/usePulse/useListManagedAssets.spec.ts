import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { ManagedAssetList } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import { handlers as pulseAssetsHandlers } from '@/api/hooks/usePulse/__handlers__'

describe('useListManagedAssets', () => {
  beforeEach(() => {
    server.use(...pulseAssetsHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data ', async () => {
    const { result } = renderHook(() => useListManagedAssets(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<ManagedAssetList>({
      items: [
        expect.objectContaining({
          id: '3b028f58-f949-4de1-9b8b-c1a35b1643a4',
          description: 'The short description of the asset',
          name: 'Test asset',
          topic: 'test/topic',
        }),
        expect.objectContaining({
          id: '3b028f58-f949-4de1-9b8b-c1a35b1643a5',
          description: 'The short description of the mapped asset',
          name: 'Test mapped asset',
          topic: 'test/topic/2',
          mapping: {
            instructions: [],
            primary: {
              id: 'test',
              type: 'TOPIC_FILTER',
            },
            sources: [
              {
                id: 'test',
                type: 'TOPIC_FILTER',
              },
              {
                id: 'test/2',
                type: 'TAG',
              },
              {
                id: 'test/3',
                type: 'PULSE_ASSET',
              },
            ],
            status: 'STREAMING',
          },
        }),
        expect.objectContaining({
          description: 'The short name of the mapped asset',
          id: '3b028f58-f949-4de1-9b8b-c1a35b1643a9',
          name: 'Test other asset',
          topic: 'test/topic/2',
        }),
        expect.objectContaining({
          id: '3b028f58-f949-4de1-9b8b-c1a35b1643a7',
          name: 'Almost the same asset',
          description: 'Not sure how to describe that re-mapped asset',
          topic: 'test/topic/4',
        }),
      ],
    })
  })
})
