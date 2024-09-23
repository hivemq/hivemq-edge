import { http, HttpResponse } from 'msw'
import type { TagSchema } from '@/api/__generated__'
import type { MQTTSample } from '@/hooks/usePrivateMqttClient/type.ts'
import { payloadToSchema } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

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

    http.get('**/management/domain/tags/schema', () => {
      return HttpResponse.json<TagSchema>([], { status: 200 })
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
      console.log('xxx')

      return HttpResponse.json({ d: 1 }, { status: 404 })
    }),
  ]
}
