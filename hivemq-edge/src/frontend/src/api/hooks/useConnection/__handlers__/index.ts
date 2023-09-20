import { rest } from 'msw'
import { Status, StatusList } from '@/api/__generated__'
import { MOCK_ADAPTER_ID, MOCK_BRIDGE_ID } from '@/__test-utils__/mocks.ts'

export const mockBridgeConnectionStatus: Status = {
  id: MOCK_BRIDGE_ID,
  connection: Status.connection.CONNECTED,
  type: 'bridge',
}

export const mockAdapterConnectionStatus: Status = {
  id: MOCK_ADAPTER_ID,
  connection: Status.connection.CONNECTED,
  type: 'adapter',
}

export const handlers = [
  rest.get('*/management/bridges/status', (_, res, ctx) => {
    return res(
      ctx.json<StatusList>({
        items: [mockBridgeConnectionStatus],
      }),
      ctx.status(200)
    )
  }),

  rest.get('*/management/protocol-adapters/status', (_, res, ctx) => {
    return res(
      ctx.json<StatusList>({
        items: [mockBridgeConnectionStatus],
      }),
      ctx.status(200)
    )
  }),
]
