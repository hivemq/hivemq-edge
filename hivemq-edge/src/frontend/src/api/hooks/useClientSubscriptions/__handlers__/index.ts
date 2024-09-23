import { delay, http, HttpResponse } from 'msw'
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

export const MOCK_SAMPLE_REFETCH_TRIGGER = 5000
export const MOCK_CLIENT_STUB = 'tmp'

export const MOCK_MQTT_TOPIC_SAMPLES = [
  `${MOCK_CLIENT_STUB}/broker1/topic1/segment1`,
  `${MOCK_CLIENT_STUB}/broker1/topic1/segment2`,
  `${MOCK_CLIENT_STUB}/broker1/topic1/segment2/leaf1`,
  `${MOCK_CLIENT_STUB}/broker2/topic1`,
  `${MOCK_CLIENT_STUB}/broker4/topic1/segment2`,
]

export const handlers = [
  http.get('**/management/client/filters', () => {
    return HttpResponse.json<ClientFilterList>([mockClientSubscription], { status: 200 })
  }),

  http.post('**/management/client/filters', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('*/management/client/filters/:clientFilterId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('*/management/client/filters/:clientFilterId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.get('**/management/client/topic-samples', async ({ request }) => {
    const url = new URL(request.url)
    const queryTime = Number(url.searchParams.get('queryTime'))

    // This "trick" is to differentiate between initial values and user-triggered request
    if (queryTime === MOCK_SAMPLE_REFETCH_TRIGGER) await delay(2000)

    return HttpResponse.json<ClientTopicList>(
      { items: queryTime === MOCK_SAMPLE_REFETCH_TRIGGER ? MOCK_MQTT_TOPIC_SAMPLES : [] },
      { status: 200 }
    )
  }),
]
