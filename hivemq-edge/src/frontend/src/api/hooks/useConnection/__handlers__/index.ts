import { rest } from 'msw'
import { ConnectionStatus, ConnectionStatusList } from '@/api/__generated__'
import { MOCK_ADAPTER_ID, MOCK_BRIDGE_ID } from '@/__test-utils__/mocks.ts'

export const mockBridgeConnectionStatus: ConnectionStatus = {
  id: MOCK_BRIDGE_ID,
  status: ConnectionStatus.status.CONNECTED,
  type: 'bridge',
}

export const mockAdapterConnectionStatus: ConnectionStatus = {
  id: MOCK_ADAPTER_ID,
  status: ConnectionStatus.status.CONNECTED,
  type: 'adapter',
}

export const handlers = [
  rest.get('*/management/bridges/status', (_, res, ctx) => {
    return res(
      ctx.json<ConnectionStatusList>({
        items: [mockBridgeConnectionStatus],
      }),
      ctx.status(200)
    )
  }),

  rest.get('*/management/protocol-adapters/status', (_, res, ctx) => {
    return res(
      ctx.json<ConnectionStatusList>({
        items: [mockBridgeConnectionStatus],
      }),
      ctx.status(200)
    )
  }),
]
