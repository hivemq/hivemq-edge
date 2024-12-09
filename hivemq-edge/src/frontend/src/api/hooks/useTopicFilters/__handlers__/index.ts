import { http, HttpResponse } from 'msw'
import { type TopicFilter, type TopicFilterList } from '@/api/__generated__'

export const MOCK_TOPIC_FILTER_SCHEMA_INVALID = 'data:application/json;base64,ewogICJ0ZXN0IjogMQp9Cg=='
export const MOCK_TOPIC_FILTER_SCHEMA_VALID =
  'data:application/json;base64,ewogICJ0eXBlIjogIm9iamVjdCIsCiAgInRpdGxlIjogIlRoaXMgaXMgYSBzaW1wbGUgc2NoZW1hIiwKICAicHJvcGVydGllcyI6IHsKICAgICJkZXNjcmlwdGlvbiI6IHsKICAgICAgInR5cGUiOiAic3RyaW5nIiwKICAgICAgInRpdGxlIjogImRlc2NyaXB0aW9uIiwKICAgICAgImRlc2NyaXB0aW9uIjogIlRoZSBkZXNjcmlwdGlvbiBvZiB0aGUgaXRlbSIKICAgIH0sCiAgICAibmFtZSI6IHsKICAgICAgInR5cGUiOiAic3RyaW5nIiwKICAgICAgInRpdGxlIjogIm5hbWUiLAogICAgICAiZGVzY3JpcHRpb24iOiAiVGhlIG5hbWUgb2YgdGhlIGl0ZW0iCiAgICB9CiAgfSwKICAicmVxdWlyZWQiOiBbCiAgICAiZGVzY3JpcHRpb24iLAogICAgIm5hbWUiCiAgXQp9Cg=='

export const MOCK_TOPIC_FILTER: TopicFilter = {
  topicFilter: 'a/topic/+/filter',
  description: 'This is a topic filter',
  schema: MOCK_TOPIC_FILTER_SCHEMA_VALID,
}

export const handlers = [
  http.post('**/management/topic-filters', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('**/management/topic-filters/:name', ({ params }) => {
    const { name } = params

    return HttpResponse.json({ name: name }, { status: 200 })
  }),

  http.put('**/management/topic-filters/:name', ({ params }) => {
    const { name } = params

    return HttpResponse.json({ name: name }, { status: 200 })
  }),

  http.get('**/management/topic-filters', () => {
    return HttpResponse.json<TopicFilterList>({ items: [MOCK_TOPIC_FILTER] }, { status: 200 })
  }),

  http.put('**/management/topic-filters', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
