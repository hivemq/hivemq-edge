import { NODE_PULSE_AGENT_DEFAULT_ID } from '@/modules/Workspace/utils/nodes-utils.ts'
import { factory, primaryKey } from '@mswjs/data'
import { http, HttpResponse } from 'msw'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import type { Combiner, CombinerList } from '@/api/__generated__'

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
        id: 'opcua-boiler1',
      },
      {
        type: EntityType.ADAPTER,
        id: 'my-other-adapter',
      },
      {
        type: EntityType.PULSE_AGENT,
        id: NODE_PULSE_AGENT_DEFAULT_ID,
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

export const persistHandlers = () => {
  const mswDB = factory({
    assetMapper: {
      id: primaryKey(String),
      mapper: String,
    },
  })

  // Create a fake one
  mswDB.assetMapper.create({
    id: MOCK_ASSET_MAPPER.id,
    mapper: JSON.stringify(MOCK_ASSET_MAPPER),
  })

  return [
    /**
     * @see useListAssetMappers
     */
    http.get('*/management/pulse/asset-mappers', () => {
      const data = mswDB.assetMapper.getAll()
      const hhhh = data.map((e) => JSON.parse(e.mapper))
      return HttpResponse.json<CombinerList>(
        {
          items: hhhh,
        },
        { status: 200 }
      )
    }),

    /**
     * @see useGetAssetMapper
     */
    http.get<CombinerParams>('*/management/pulse/asset-mappers/:combinerId', ({ params }) => {
      const { combinerId } = params
      const data = mswDB.assetMapper.findFirst({
        where: {
          id: {
            equals: combinerId,
          },
        },
      })
      if (data?.mapper)
        return HttpResponse.json<Combiner>({ ...JSON.parse(data?.mapper), id: combinerId }, { status: 200 })
      else HttpResponse.json({ error: 'not found' }, { status: 400 })
    }),

    /**
     * @see useCreateAssetMapper
     */
    http.post<never, Combiner>('*/management/pulse/asset-mappers', async ({ request }) => {
      const data = await request.json()
      mswDB.assetMapper.create({
        id: data.id,
        mapper: JSON.stringify(data),
      })
      return HttpResponse.json({ created: data }, { status: 200 })
    }),

    /**
     * @see useDeleteAssetMapper
     */
    http.delete<CombinerParams>('*/management/pulse/asset-mappers/:combinerId', ({ params }) => {
      const { combinerId } = params
      const data = mswDB.assetMapper.delete({
        where: {
          id: {
            equals: combinerId,
          },
        },
      })
      if (data?.mapper) return HttpResponse.json({ deleted: combinerId }, { status: 200 })
      else HttpResponse.json({ error: 'not found' }, { status: 400 })
    }),

    /**
     * @see useUpdateAssetMapper
     */
    http.put<CombinerParams, Combiner>('*/management/pulse/asset-mappers/:combinerId', async ({ params, request }) => {
      const { combinerId } = params
      const data = await request.json()

      const updatedData = mswDB.assetMapper.update({
        where: {
          id: {
            equals: combinerId,
          },
        },
        data,
      })
      if (updatedData?.mapper) return HttpResponse.json({ updated: updatedData, id: combinerId }, { status: 200 })
      else HttpResponse.json({ error: 'not found' }, { status: 400 })
    }),
  ]
}
