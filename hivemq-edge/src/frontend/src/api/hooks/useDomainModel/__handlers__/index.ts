import { http, HttpResponse } from 'msw'
import type { TagSchema } from '@/api/__generated__'

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
