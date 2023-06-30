import { Bridge, BridgeList } from '@/api/__generated__'
import { rest } from 'msw'

export const mockBridge: Bridge = {
  host: 'my.h0st.org',
  keepAlive: 0,
  id: 'bridge-id-01',
  port: 0,
  sessionExpiry: 0,
  cleanStart: true,
  clientId: 'my-client-id',
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
