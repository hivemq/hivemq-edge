import { rest } from 'msw'
import { RJSFSchema } from '@rjsf/utils'

import { type Schema, SchemaList } from '@/api/__generated__'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

export const MOCK_SCHEMA_ID = 'my-schema-id'
export const MOCK_SCHEMA_SOURCE: RJSFSchema = {
  title: 'Valid Sensor Data',
  description: 'A schema that matches the temperature and humidity values of any object',
  required: ['temperature', 'humidity'],
  type: 'object',
  properties: {
    temperature: {
      type: 'number',
      minimum: 20,
      maximum: 70,
    },
    humidity: {
      type: 'number',
      minimum: 65,
      maximum: 100,
    },
  },
}

export const mockSchemaTempHumidity: Schema = {
  id: MOCK_SCHEMA_ID,
  createdAt: MOCK_CREATED_AT,
  schemaDefinition: btoa(JSON.stringify(MOCK_SCHEMA_SOURCE)),
  //TODO[NVL] Should be typed (enum): JSON, PROTOBUF
  type: 'JSON',
}

export const handlers = [
  rest.get('*/data-hub/schemas', (_, res, ctx) => {
    return res(
      ctx.json<SchemaList>({
        items: [mockSchemaTempHumidity],
      }),
      ctx.status(200)
    )
  }),
]
