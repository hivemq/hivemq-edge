import { Bridge, BridgeList, Status } from '@/api/__generated__'
import { rest } from 'msw'
import { MOCK_TOPIC_ACT1, MOCK_TOPIC_ALL, MOCK_TOPIC_BRIDGE_DESTINATION } from '@/__test-utils__/react-flow/topics.ts'

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
      filters: [MOCK_TOPIC_ACT1],
      destination: MOCK_TOPIC_BRIDGE_DESTINATION,
      maxQoS: 0,
    },
  ],
}

export const handlers = [
  rest.get('*/bridges', (_, res, ctx) => {
    return res(ctx.json<BridgeList>({ items: [mockBridge] }), ctx.status(200))
  }),

  rest.get('*/bridges/:bridgeId', (req, res, ctx) => {
    const { bridgeId } = req.params
    return res(ctx.json<Bridge>({ ...mockBridge, id: bridgeId as string }), ctx.status(200))
  }),

  rest.post('*/bridges', (_, res, ctx) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),

  rest.delete('*/bridges/:bridgeId', (req, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { bridgeId } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),

  rest.put('*/bridges/:bridgeId', (req, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { bridgeId } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),
]
