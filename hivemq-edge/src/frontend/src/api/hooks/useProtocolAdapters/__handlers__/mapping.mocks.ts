import { http, HttpResponse } from 'msw'
import type { SouthboundMappingList, SouthboundMapping, NorthboundMappingList, JsonNode } from '@/api/__generated__'
import { NorthboundMapping } from '@/api/__generated__'

import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_MAX_QOS } from '@/__test-utils__/adapters/mqtt.ts'

export const MOCK_SOUTHBOUND_MAPPING: SouthboundMapping = {
  topicFilter: 'my/filter',
  tagName: 'my/tag',
  fieldMapping: {
    instructions: [{ source: 'dropped-property', destination: 'lastName' }],
    metadata: { destination: GENERATE_DATA_MODELS(true, 'my/filter'), source: GENERATE_DATA_MODELS(true, 'my/tag') },
  },
}

export const MOCK_NORTHBOUND_MAPPING: NorthboundMapping = {
  tagName: 'my/tag',
  topic: 'my/topic',
  includeTagNames: true,
  includeTimestamp: true,
  maxQoS: MOCK_MAX_QOS,
  messageExpiryInterval: -1000,
  messageHandlingOptions: NorthboundMapping.messageHandlingOptions.MQTTMESSAGE_PER_TAG,
}

export const mappingHandlers = [
  http.get<{ adapterId: string; tagName: string }>(
    '*/management/protocol-adapters/writing-schema/:adapterId/:tagName',
    ({ params }) => {
      const { tagName } = params
      return HttpResponse.json<JsonNode>(GENERATE_DATA_MODELS(true, tagName), { status: 200 })
    }
  ),

  http.get<{ adapterId: string }>('*/management/protocol-adapters/adapters/:adapterId/southboundMappings', () => {
    return HttpResponse.json<SouthboundMappingList>({ items: [MOCK_SOUTHBOUND_MAPPING] }, { status: 200 })
  }),

  http.get<{ adapterId: string }>('*/management/protocol-adapters/adapters/:adapterId/northboundMappings', () => {
    return HttpResponse.json<NorthboundMappingList>({ items: [MOCK_NORTHBOUND_MAPPING] }, { status: 200 })
  }),

  http.put<{ adapterId: string }>(
    '*/management/protocol-adapters/adapters/:adapterId/northboundMappings',
    async ({ params, request }) => {
      const { adapterId } = params
      const { items } = (await request.json()) as NorthboundMappingList

      return HttpResponse.json(
        {
          adapterId,
          items: items.map((item) => {
            const { topic, tagName } = item
            return { topic, tagName }
          }),
        },
        { status: 200 }
      )
    }
  ),

  http.put<{ adapterId: string }>(
    '*/management/protocol-adapters/adapters/:adapterId/southboundMappings',
    async ({ params, request }) => {
      const { adapterId } = params
      const { items } = (await request.json()) as SouthboundMappingList

      return HttpResponse.json(
        {
          adapterId,
          items: items.map((item) => {
            const { topicFilter, tagName } = item
            return { topicFilter, tagName }
          }),
        },
        { status: 200 }
      )
    }
  ),
]

export const safeWritingSchemaHandlers = [
  http.get<{ adapterId: string; tagName: string }>(
    '*/management/protocol-adapters/writing-schema/:adapterId/:tagName',
    ({ params }) => {
      const { tagName } = params
      return HttpResponse.json<JsonNode>(GENERATE_DATA_MODELS(true, tagName), { status: 200 })
    }
  ),
]
