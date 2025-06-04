import { http, HttpResponse } from 'msw'

import { MOCK_TOPIC_ALL, MOCK_TOPIC_BRIDGE_DESTINATION, MOCK_TOPIC_REF1 } from '@/__test-utils__/react-flow/topics.ts'
import type { Bridge, BridgeList, StatusList } from '@/api/__generated__'
import { Status, StatusTransitionResult } from '@/api/__generated__'
import { mockBridgeConnectionStatus } from '@/api/hooks/useConnection/__handlers__'

export const mockBridgeId = 'bridge-id-01'

export const mockBridgeStatusTransition: StatusTransitionResult = {
  callbackTimeoutMillis: 2000,
  identifier: mockBridgeId,
  status: StatusTransitionResult.status.PENDING,
  type: 'adapter',
}

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

  http.put('*/bridges/:bridgeId/status', ({ params }) => {
    const { bridgeId } = params
    return HttpResponse.json<StatusTransitionResult>(
      { ...mockBridgeStatusTransition, identifier: bridgeId as string },
      { status: 200 }
    )
  }),

  http.get('*/bridges/:bridgeId/connection-status', () => {
    return HttpResponse.json<StatusList>(
      {
        items: [mockBridgeConnectionStatus],
      },
      { status: 200 }
    )
  }),
]
