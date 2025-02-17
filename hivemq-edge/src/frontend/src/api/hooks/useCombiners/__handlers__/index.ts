import { http, HttpResponse } from 'msw'
import type { Combiner, CombinerList, DataCombining, DataCombiningList, Instruction } from '../../../__generated__'
import { EntityType } from '@/api/__generated__'

interface CombinerParams {
  combinerId: string
}

interface MappingParams extends CombinerParams {
  mappingId: string
}

const mockCombinerId = '6991ff43-9105-445f-bce3-976720df40a3'

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
}

export const mockCombinerMapping: DataCombining = {
  id: '58677276-fc48-4a9a-880c-41c755f5063b',
  sources: {
    tags: [],
    topicFilters: [],
  },
  destination: 'my/topic',
  instructions: [],
}

export const handlers = [
  http.get('*/management/combiners', () => {
    return HttpResponse.json<CombinerList>({ items: [mockCombiner] }, { status: 200 })
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
