import { rest } from 'msw'
import { ConnectionStatus, ConnectionStatusList } from '@/api/__generated__'

export const mockConnectionStatus: ConnectionStatus = {
  id: 'first-bridge',
  status: ConnectionStatus.status.CONNECTED,
  type: 'bridge',
}

export const handlers = [
  rest.get('*/management/bridges/status', (_, res, ctx) => {
    return res(
      ctx.json<ConnectionStatusList>({
        items: [mockConnectionStatus],
      }),
      ctx.status(200)
    )
  }),

  rest.get('*/management/protocol-adapters/status', (_, res, ctx) => {
    return res(
      ctx.json<ConnectionStatusList>({
        items: [mockConnectionStatus],
      }),
      ctx.status(200)
    )
  }),
]
