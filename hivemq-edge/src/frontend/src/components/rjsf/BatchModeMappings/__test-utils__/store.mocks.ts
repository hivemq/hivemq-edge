import type { IdSchema } from '@rjsf/utils'
import type { RJSFSchema } from '@rjsf/utils/src/types.ts'
import type { ColumnMappingData, WorksheetData } from '@/components/rjsf/BatchModeMappings/types.ts'

export const MOCK_ID_SCHEMA: IdSchema<unknown> = { $id: 'my-id' }
export const MOCK_SCHEMA: RJSFSchema = {
  type: 'array',
  items: {
    type: 'object',
    properties: {
      'message-expiry-interval': {
        type: 'integer',
        title: 'MQTT message expiry interval [s]',
      },
      'mqtt-topic': {
        type: 'string',
        title: 'Destination MQTT topic',
      },
      node: {
        type: 'string',
        title: 'Source Node ID',
      },
      'publishing-interval': {
        type: 'integer',
        title: 'OPC UA publishing interval [ms]',
      },
    },
    required: ['mqtt-topic', 'node'],
  },
}
export const MOCK_WORKSHEET: WorksheetData[] = [
  {
    a: 1,
    b: 2,
    c: 3,
    d: 4,
  },
  {
    a: 2,
    b: 3,
    c: 3,
    d: 5,
  },
]
export const MOCK_MAPPING: ColumnMappingData[] = [
  {
    column: 'a',
    subscription: 'Destination MQTT topic',
  },
  {
    column: 'd',
    subscription: 'Source Node ID',
  },
]
