import { http, HttpResponse } from 'msw'
import { RJSFSchema } from '@rjsf/utils'

import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'
import type { DomainTag, DomainTagList, JsonNode, PayloadSampleList, TagSchema } from '@/api/__generated__'
import {
  MOCK_DEVICE_TAG_ADDRESS_MODBUS,
  MOCK_DEVICE_TAG_FAKE,
  MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import { payloadToSchema } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import type { MQTTSample } from '@/hooks/usePrivateMqttClient/type.ts'

export const GENERATE_DATA_MODELS = (short = false, title?: string): RJSFSchema => {
  const model: RJSFSchema = {
    title: title || 'A registration form',
    description: 'A simple form example.',
    type: 'object',
    required: ['firstName', 'lastName'],
    properties: {
      firstName: {
        type: 'string',
        title: short ? 'First String' : 'firstName',
        default: 'Chuck',
        examples: 'firstName',
      },
      lastName: {
        type: 'string',
        title: short ? 'Second String' : 'lastname',
      },
      age: {
        type: 'integer',
        title: short ? 'Integer' : 'age',
      },
      weight: {
        type: 'number',
        title: short ? 'Number' : 'age',
      },
      subItems: {
        type: 'object',
        title: 'subItems',
        examples: 'subItems',

        properties: {
          name: {
            examples: 'name',
            type: 'string',
            title: 'name',
            default: 'Default name',
          },
          type: {
            type: 'string',
            title: 'type',
            default: 'Default type',
          },
        },
      },
      array: {
        type: 'array',
        items: {
          type: 'string',
        },
      },
    },
  }

  if (!short)
    model.properties = {
      ...model.properties,
      bio: {
        type: 'string',
        title: 'bio',
      },
      password: {
        type: 'string',
        title: 'password',
        minLength: 3,
      },
      telephone: {
        type: 'string',
        title: 'telephone',
        minLength: 10,
      },
    }

  return model
}

export const handlers = [
  http.get<{ protocolId: string }>('**/management/protocol-adapters/tag-schemas/:protocolId', ({ params }) => {
    const { protocolId } = params

    if (protocolId === MockAdapterType.ADS)
      return HttpResponse.json<ProblemDetails>(
        { title: 'The schema for the tags cannot be found', status: 404 },
        { status: 404 }
      )

    return HttpResponse.json<TagSchema>(MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA, { status: 200 })
  }),

  http.get<{ tagName: string }>('**/management/protocol-adapters/tags/:tagName', ({ params }) => {
    const { tagName } = params

    try {
      const realTag = atob(tagName)
      if (realTag === MOCK_DEVICE_TAG_FAKE)
        return HttpResponse.json<ProblemDetails>({ title: 'The tag is not found', status: 404 }, { status: 404 })
      return HttpResponse.json<DomainTag>(
        { name: realTag, definition: MOCK_DEVICE_TAG_ADDRESS_MODBUS },
        { status: 200 }
      )
    } catch (e) {
      return HttpResponse.json<ProblemDetails>({ title: 'The tag is not well formed', status: 400 }, { status: 400 })
    }
  }),

  http.get<{ topic: string }>('**/management/sampling/topic/:topic', ({ params }) => {
    const { topic } = params

    if (topic === MOCK_DEVICE_TAG_FAKE)
      return HttpResponse.json<ProblemDetails>(
        { title: 'The schema for the tags cannot be found', status: 404 },
        { status: 404 }
      )

    return HttpResponse.json<PayloadSampleList>({ items: [] }, { status: 200 })
  }),

  http.get<{ topic: string }>('**/management/sampling/schema/:topic', ({ params }) => {
    const { topic } = params

    if (topic === MOCK_DEVICE_TAG_FAKE)
      return HttpResponse.json<ProblemDetails>(
        { title: 'The schema for the tags cannot be found', status: 404 },
        { status: 404 }
      )

    return HttpResponse.json<JsonNode>(GENERATE_DATA_MODELS(true, topic), { status: 200 })
  }),

  http.get('**/management/protocol-adapters/tags', () => {
    return HttpResponse.json<DomainTagList>(
      {
        items: [{ name: 'test/tag1', definition: MOCK_DEVICE_TAG_ADDRESS_MODBUS }],
      },
      { status: 200 }
    )
  }),

  http.post<{ topic: string }>('**/management/sampling/topic/:topic', ({ params }) => {
    const { topic } = params

    return HttpResponse.json({ topic }, { status: 200 })
  }),
]

export const schemaHandlers = (onSampling?: (topicFilter: string) => Promise<MQTTSample[]> | undefined) => {
  return [
    http.get('**/management/domain/tags', () => {
      return HttpResponse.json<Array<string>>([], { status: 200 })
    }),

    http.get('**/management/domain/tags/schema', ({ request }) => {
      const url = new URL(request.url)
      const tags = url.searchParams.getAll('tags')
      return HttpResponse.json<TagSchema>(
        { configSchema: GENERATE_DATA_MODELS(true, tags[0]), protocolId: 'protocol' },
        { status: 200 }
      )
    }),

    http.get('**/management/domain/topics', () => {
      return HttpResponse.json<Array<string>>([], { status: 200 })
    }),

    http.get('**/management/domain/topics/schema', async ({ request }) => {
      const url = new URL(request.url)
      const topics = url.searchParams.getAll('topics')
      if (topics.length && onSampling) {
        const samples = await onSampling(topics[0])
        const schemas = payloadToSchema(samples)
        return HttpResponse.json<TagSchema>(schemas, { status: 200 })
      }
      return HttpResponse.json({ d: 1 }, { status: 404 })
    }),
  ]
}

export const safeTopicSchemaHandlers = [
  http.get<{ topic: string }>('**/management/sampling/schema/:topic', ({ params }) => {
    const { topic } = params

    if (topic === MOCK_DEVICE_TAG_FAKE)
      return HttpResponse.json<ProblemDetails>(
        { title: 'The schema for the tags cannot be found', status: 404 },
        { status: 404 }
      )

    return HttpResponse.json<JsonNode>(GENERATE_DATA_MODELS(true, topic), { status: 200 })
  }),
]
