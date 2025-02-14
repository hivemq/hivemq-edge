import { http, HttpResponse } from 'msw'
import type { Combiner, CombinerList } from '../../../__generated__'

interface CombinerParams {
  combinerId: string
}

const mockCombinerId = '6991ff43-9105-445f-bce3-976720df40a3'

export const mockCombiner: Combiner = {
  id: mockCombinerId,
  name: 'my-combiner',
}

export const handlers = [
  http.get('*/management/combiners', () => {
    return HttpResponse.json<CombinerList>({ items: [mockCombiner] }, { status: 200 })
  }),

  http.get<CombinerParams>('*/management/combiners/:combinerId', ({ params }) => {
    const { combinerId } = params
    return HttpResponse.json<Combiner>({ ...mockCombiner, id: combinerId }, { status: 200 })
  }),

  http.post<never, Combiner>('*/management/combiners', ({ request }) => {
    const { body } = request
    return HttpResponse.json({ created: body }, { status: 200 })
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
