import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import type { CombinerList } from '@/api/__generated__'
import { EntityType, DataIdentifierReference } from '@/api/__generated__'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'

import { useListCombiners } from './useListCombiners'

describe('useListCombiners', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useListCombiners(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<CombinerList>({
      items: [
        {
          id: '6991ff43-9105-445f-bce3-976720df40a3',
          name: 'my-combiner',
          sources: {
            items: expect.arrayContaining([
              {
                id: 'my-adapter',
                type: 'ADAPTER',
              },
              {
                id: 'my-other-adapter',
                type: 'ADAPTER',
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
        {
          id: '5e08d9f3-113d-46f2-8418-9a8bf980cc10',
          name: 'fake1',
          sources: { items: [] },
          mappings: { items: [] },
        },
        {
          id: '2d2ec927-1ff5-4e1a-b307-ab135cc189fd',
          name: 'fake2',
          sources: {
            items: [
              {
                id: '444',
                type: EntityType.ADAPTER,
              },
            ],
          },
          mappings: { items: [] },
        },
      ],
    })
  })
})
