import { http, HttpResponse } from 'msw'
import type { FieldMappingsListModel, FieldMappingsModel, JsonNode } from '@/api/__generated__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

export const MOCK_MAPPING: FieldMappingsModel = {
  topicFilter: 'my/filter',
  tag: 'my/tag',
  metadata: { destination: GENERATE_DATA_MODELS(true, 'my/filter'), source: GENERATE_DATA_MODELS(true, 'my/tag') },
  fieldMapping: [],
}

export const mappingHandlers = [
  http.get<{ adapterId: string }>('*/management/protocol-adapters/adapters/:adapterId/fieldmappings', () => {
    return HttpResponse.json<FieldMappingsListModel>({ items: [MOCK_MAPPING] }, { status: 200 })
  }),

  http.get<{ adapterId: string; tagName: string }>(
    '*/management/protocol-adapters/writing-schema/:adapterId/:tagName',
    ({ params }) => {
      const { tagName } = params
      return HttpResponse.json<JsonNode>(GENERATE_DATA_MODELS(true, tagName), { status: 200 })
    }
  ),
]
