import { http, HttpResponse } from 'msw'
import { ClientFilter, ClientFilterList, type ClientTopicList } from '@/api/__generated__'

export const mockClientSubscription: ClientFilter = {
  topicFilters: [{ destination: 'test/topic/1' }],
  id: 'my-first-client',
}

export const mockClientSubscriptionsList: ClientFilter[] = [
  mockClientSubscription,
  {
    topicFilters: [],
    id: 'my-first-client2',
  },
]

export const mockClientTopicList: string[] = ['client1/topic/1', 'client1/topic/2', 'client1/topic/3']

export const handlers = [
  http.get('**/management/client/filters', () => {
    return HttpResponse.json<ClientFilterList>([mockClientSubscription], { status: 200 })
  }),

  http.post('**/management/client/filters', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.get('**/management/client/topic-samples', () => {
    return HttpResponse.json<ClientTopicList>({ items: mockClientTopicList }, { status: 200 })
  }),
]
