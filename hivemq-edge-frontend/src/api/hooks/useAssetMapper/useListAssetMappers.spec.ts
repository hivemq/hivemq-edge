import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import type { CombinerList } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'

import { useListAssetMappers } from './useListAssetMappers'

describe('useListAssetMappers', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useListAssetMappers(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<CombinerList>({
      items: [
        {
          id: 'ba4f7882-f7c0-4ce7-bf65-485677fc1b60',
          name: 'my-asset-mapper',
          description: 'This is a description',

          sources: {
            items: expect.arrayContaining([
              {
                id: 'opcua-boiler1',
                type: 'ADAPTER',
              },
              {
                id: 'my-other-adapter',
                type: 'ADAPTER',
              },
              {
                id: 'idPulse',
                type: 'PULSE_AGENT',
              },
            ]),
          },
          mappings: {
            items: [
              {
                destination: { topic: 'my/first/topic' },
                id: '3b028f58-f949-4de1-9b8b-c1a35b1643a4',
                instructions: [],
                sources: {
                  primary: { id: '', type: DataIdentifierReference.type.TAG },
                  tags: ['my/tag/t1', 'my/tag/t3'],
                  topicFilters: ['my/topic/+/temp'],
                },
              },
              {
                destination: { topic: 'my/other/topic' },
                id: 'c02a9d0f-02cb-4ff0-a7b4-6e1a16b08722',
                instructions: [],
                sources: {
                  primary: { id: '', type: DataIdentifierReference.type.TAG },
                  tags: [],
                  topicFilters: [],
                },
              },
            ],
          },
        },
      ],
    })
  })
})
