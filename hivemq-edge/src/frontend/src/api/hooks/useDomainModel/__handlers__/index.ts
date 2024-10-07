import { http, HttpResponse } from 'msw'
import { RJSFSchema } from '@rjsf/utils'

import type { TagSchema } from '@/api/__generated__'
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
  http.get('**/management/domain/tags', () => {
    return HttpResponse.json<Array<string>>([], { status: 200 })
  }),

  http.get('**/management/domain/tags/schema', () => {
    return HttpResponse.json<TagSchema>([], { status: 200 })
  }),

  http.get('**/management/domain/topics', () => {
    return HttpResponse.json<Array<string>>([], { status: 200 })
  }),

  http.get('**/management/domain/topics/schema', () => {
    return HttpResponse.json<TagSchema>([], { status: 200 })
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
      return HttpResponse.json<TagSchema>(GENERATE_DATA_MODELS(true, tags[0]), { status: 200 })
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
