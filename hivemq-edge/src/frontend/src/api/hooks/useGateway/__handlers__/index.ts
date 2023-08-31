import { rest } from 'msw'
import { Listener, ListenerList } from '@/api/__generated__'

export const mockMqttListener: Listener = {
  name: 'tcp-listener-1883',
  hostName: '127.0.0.1',
  port: 1883,
  description: 'MQTT TCP Listener',
  transport: Listener.transport.TCP,
  protocol: 'mqtt',
}

export const handlers = [
  rest.get('*/gateway/listeners', (_, res, ctx) => {
    return res(
      ctx.json<ListenerList>({
        items: [mockMqttListener],
      }),
      ctx.status(200)
    )
  }),
]
