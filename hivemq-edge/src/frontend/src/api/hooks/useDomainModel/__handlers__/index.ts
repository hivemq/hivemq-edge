import { http, HttpResponse } from 'msw'
import type { TagSchema } from '@/api/__generated__'
import { MQTTSample } from '@/hooks/usePrivateMqttClient/type.ts'

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
  if (onSampling) {
    const sss = onSampling('pumps/configuration')
    console.log('is Sampling ready?', sss)
    sss?.then((w) => console.log('XXXXXXXXX meg', w)).catch((w) => console.log('XXXXXXX err', w))
  }
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

    http.get('**/management/domain/topics/schema', () => {
      return HttpResponse.json<TagSchema>([], { status: 200 })
    }),
  ]
}
