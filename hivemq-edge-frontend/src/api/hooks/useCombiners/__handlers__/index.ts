import { http, HttpResponse } from 'msw'
import { DataIdentifierReference } from '@/api/__generated__'
import type {
  Combiner,
  CombinerList,
  DataCombiningList,
  EntityReference,
  Instruction,
  DataCombining,
} from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'

interface CombinerParams {
  combinerId: string
}

interface MappingParams extends CombinerParams {
  mappingId: string
}

export const mockCombinerId = '6991ff43-9105-445f-bce3-976720df40a3'

export const mockEntityReference: EntityReference = {
  type: EntityType.ADAPTER,
  id: 'my-adapter',
}

export const mockCombiner: Combiner = {
  id: mockCombinerId,
  name: 'my-combiner',
  sources: {
    items: [
      {
        type: EntityType.ADAPTER,
        id: 'my-adapter',
      },
      {
        type: EntityType.ADAPTER,
        id: 'my-other-adapter',
      },
    ],
  },
  mappings: {
    items: [
      {
        id: '3b028f58-f949-4de1-9b8b-c1a35b1643a4',
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
          tags: ['my/tag/t1', 'my/tag/t3'],
          topicFilters: ['my/topic/+/temp'],
        },
        destination: { topic: 'my/first/topic' },
        instructions: [],
      },
      {
        id: 'c02a9d0f-02cb-4ff0-a7b4-6e1a16b08722',
        sources: { primary: { id: '', type: DataIdentifierReference.type.TAG }, tags: [], topicFilters: [] },
        destination: { topic: 'my/other/topic' },
        instructions: [],
      },
    ],
  },
}

export const mockEmptyCombiner: Combiner = {
  id: mockCombinerId,
  name: 'my-combiner',
  sources: {
    items: [],
  },
  mappings: {
    items: [],
  },
}

export const mockCombinerMapping: DataCombining = {
  id: '58677276-fc48-4a9a-880c-41c755f5063b',
  sources: {
    primary: { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG },
    tags: ['my/tag/t1', 'my/tag/t3'],
    topicFilters: ['my/topic/+/temp'],
  },
  destination: { topic: 'my/topic' },
  instructions: [],
}

export const MOCK_COMBINER_ASSET: Combiner = {
  id: 'e9af7f82-bec1-4d07-8c0f-e4591148af19',
  name: 'my-combiner-for-asset',
  sources: {
    items: [
      {
        type: EntityType.ADAPTER,
        id: 'my-adapter',
      },
      {
        type: EntityType.PULSE_AGENT,
        id: 'the Pulse Agent',
      },
    ],
  },
  mappings: {
    items: [
      {
        id: 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb',
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
          tags: ['my/tag/t1', 'my/tag/t3'],
          topicFilters: ['my/topic/+/temp'],
        },
        destination: { topic: 'my/first/topic' },
        instructions: [],
      },
    ],
  },
}

export const handlers = [
  http.get('*/management/combiners', () => {
    return HttpResponse.json<CombinerList>(
      {
        items: [
          mockCombiner,
          {
            id: '5e08d9f3-113d-46f2-8418-9a8bf980cc10',
            name: 'fake1',
            sources: {
              items: [],
            },
            mappings: { items: [] },
          },
          {
            id: '2d2ec927-1ff5-4e1a-b307-ab135cc189fd',
            name: 'fake2',
            sources: {
              items: [
                {
                  type: EntityType.ADAPTER,
                  id: '444',
                },
              ],
            },
            mappings: { items: [] },
          },
        ],
      },
      { status: 200 }
    )
  }),

  http.get<CombinerParams>('*/management/combiners/:combinerId', ({ params }) => {
    const { combinerId } = params
    return HttpResponse.json<Combiner>({ ...mockCombiner, id: combinerId }, { status: 200 })
  }),

  http.post<never, Combiner>('*/management/combiners', async ({ request }) => {
    const data = await request.json()
    return HttpResponse.json({ created: data }, { status: 200 })
  }),

  http.delete<CombinerParams>('*/management/combiners/:combinerId', ({ params }) => {
    const { combinerId } = params
    return HttpResponse.json({ deleted: combinerId }, { status: 200 })
  }),

  http.put<CombinerParams, Combiner>('*/management/combiners/:combinerId', async ({ params, request }) => {
    const { combinerId } = params
    const data = await request.json()

    return HttpResponse.json({ updated: data, id: combinerId }, { status: 200 })
  }),

  http.get<CombinerParams>('*/management/combiners/:combinerId/mappings', () => {
    return HttpResponse.json<DataCombiningList>({ items: [mockCombinerMapping] }, { status: 200 })
  }),

  http.get<MappingParams>('*/management/combiners/:combinerId/mappings/:mappingId/instructions', () => {
    return HttpResponse.json<Array<Instruction>>([], { status: 200 })
  }),
]

export const handlerCombinerAssets = [
  http.get('*/management/combiners', () => {
    return HttpResponse.json<CombinerList>(
      {
        items: [MOCK_COMBINER_ASSET],
      },
      { status: 200 }
    )
  }),
]
