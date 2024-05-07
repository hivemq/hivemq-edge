import { Bridge, BridgeList, Status } from '@/api/__generated__'
import { http, HttpResponse } from 'msw'
import { MOCK_TOPIC_ALL, MOCK_TOPIC_BRIDGE_DESTINATION, MOCK_TOPIC_REF1 } from '@/__test-utils__/react-flow/topics.ts'

export const mockBridgeId = 'bridge-id-01'

export const mockBridge: Bridge = {
  host: 'my.h0st.org',
  keepAlive: 0,
  id: mockBridgeId,
  port: 0,
  sessionExpiry: 0,
  cleanStart: true,
  clientId: 'my-client-id',
  status: {
    connection: Status.connection.CONNECTED,
  },
  localSubscriptions: [
    {
      filters: [MOCK_TOPIC_ALL],
      destination: MOCK_TOPIC_BRIDGE_DESTINATION,
      maxQoS: 0,
    },
  ],
  remoteSubscriptions: [
    {
      filters: [MOCK_TOPIC_REF1],
      destination: MOCK_TOPIC_BRIDGE_DESTINATION,
      maxQoS: 0,
    },
  ],
}

export const handlers = [
  http.get('*/bridges', () => {
    return HttpResponse.json<BridgeList>({ items: [mockBridge] }, { status: 200 })
  }),

  http.get('*/bridges/:bridgeId', ({ params }) => {
    const { bridgeId } = params
    return HttpResponse.json<Bridge>({ ...mockBridge, id: bridgeId as string }, { status: 200 })
  }),

  http.post('*/bridges', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('*/bridges/:bridgeId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('*/bridges/:bridgeId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
