import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { type DomainTagList, EntityType, type TopicFilterList } from '@/api/__generated__'
import type { EntityReference } from '@/api/__generated__'
import { deviceHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers, MOCK_TOPIC_FILTER_SCHEMA_VALID } from '@/api/hooks/useTopicFilters/__handlers__'
import { useGetCombinedEntities } from './useGetCombinedEntities'

const mockEntityReferences: EntityReference[] = [
  {
    id: 'opcua',
    type: EntityType.ADAPTER,
  },
  {
    id: 'a/topic/+/filter',
    type: EntityType.BRIDGE,
  },
]

describe('useGetCombinedEntities', () => {
  beforeEach(() => {
    // server.use(...handlers)
    server.use(...deviceHandlers, ...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetCombinedEntities(mockEntityReferences), { wrapper })
    await waitFor(() => {
      expect(result.current).not.toBeUndefined()
      expect(result.current.every((e) => e.isLoading)).toBeFalsy()
    })
    expect(result.current.length).toEqual(2)

    expect(result.current[0].data).toStrictEqual<DomainTagList>({
      items: [
        {
          definition: {
            node: 'ns=3;i=1002',
          },
          name: 'opcua/power/off',
        },
        {
          definition: {
            node: 'ns=3;i=1008',
          },
          name: 'opcua/log/event',
        },
      ],
    })

    expect(result.current[1].data).toStrictEqual<TopicFilterList>({
      items: [
        {
          description: 'This is a topic filter',
          schema: MOCK_TOPIC_FILTER_SCHEMA_VALID,
          topicFilter: 'a/topic/+/filter',
        },
      ],
    })
  })
})
