import { http, HttpResponse } from 'msw'
import type { StatusList } from '@/api/__generated__'
import { Status } from '@/api/__generated__'
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
  http.get('*/management/bridges/status', () => {
    return HttpResponse.json<StatusList>(
      {
        items: [mockBridgeConnectionStatus],
      },
      { status: 200 }
    )
  }),

  http.get('*/management/protocol-adapters/status', () => {
    return HttpResponse.json<StatusList>(
      {
        items: [mockAdapterConnectionStatus],
      },
      { status: 200 }
    )
  }),
]
