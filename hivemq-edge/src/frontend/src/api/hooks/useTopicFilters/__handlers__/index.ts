import { http, HttpResponse } from 'msw'
import { type TopicFilter, type TopicFilterList } from '@/api/__generated__'

export const MOCK_TOPIC_FILTER: TopicFilter = {
  topicFilter: 'a/topic/+/filter',
  description: 'This is a topic filter',
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
