import { http, HttpResponse } from 'msw'
import { DataIdentifierReference } from '@/api/__generated__'
import type { Combiner, CombinerList } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'

interface CombinerParams {
  combinerId: string
}

export const MOCK_ASSET_MAPPER_ID = 'ba4f7882-f7c0-4ce7-bf65-485677fc1b60'

export const MOCK_ASSET_MAPPER: Combiner = {
  id: MOCK_ASSET_MAPPER_ID,
  name: 'my-asset-mapper',
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
      {
        type: EntityType.PULSE_AGENT,
        id: 'my-pulse-agent',
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

export const handlers = [
  /**
   * @see useListAssetMappers
   */
  http.get('*/management/pulse/asset-mappers', () => {
    return HttpResponse.json<CombinerList>(
      {
        items: [MOCK_ASSET_MAPPER],
      },
      { status: 200 }
    )
  }),

  /**
   * @see useGetAssetMapper
   */
  http.get<CombinerParams>('*/management/pulse/asset-mappers/:combinerId', ({ params }) => {
    const { combinerId } = params
    return HttpResponse.json<Combiner>({ ...MOCK_ASSET_MAPPER, id: combinerId }, { status: 200 })
  }),

  /**
   * @see useCreateAssetMapper
   */
  http.post<never, Combiner>('*/management/pulse/asset-mappers', async ({ request }) => {
    const data = await request.json()
    return HttpResponse.json({ created: data }, { status: 200 })
  }),

  /**
   * @see useDeleteAssetMapper
   */
  http.delete<CombinerParams>('*/management/pulse/asset-mappers/:combinerId', ({ params }) => {
    const { combinerId } = params
    return HttpResponse.json({ deleted: combinerId }, { status: 200 })
  }),

  /**
   * @see useUpdateAssetMapper
   */
  http.put<CombinerParams, Combiner>('*/management/pulse/asset-mappers/:combinerId', async ({ params, request }) => {
    const { combinerId } = params
    const data = await request.json()

    return HttpResponse.json({ updated: data, id: combinerId }, { status: 200 })
  }),
]
