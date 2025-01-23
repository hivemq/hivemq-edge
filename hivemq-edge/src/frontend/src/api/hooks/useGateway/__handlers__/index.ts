import { http, HttpResponse } from 'msw'
import type { ListenerList } from '@/api/__generated__'
import { Listener } from '@/api/__generated__'

export const mockMqttListener: Listener = {
  name: 'tcp-listener-1883',
  hostName: '127.0.0.1',
  port: 1883,
  description: 'MQTT TCP Listener',
  transport: Listener.transport.TCP,
  protocol: 'mqtt',
}

export const handlers = [
  http.get('*/gateway/listeners', () => {
    return HttpResponse.json<ListenerList>(
      {
        items: [mockMqttListener],
      },
      { status: 200 }
    )
  }),
]
