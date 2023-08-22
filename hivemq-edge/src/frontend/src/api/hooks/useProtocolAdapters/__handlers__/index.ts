import { Adapter, AdaptersList, JsonNode, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import { rest } from 'msw'

export const mockJSONSchema: JsonNode = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  type: 'object',
  properties: {
    host: {
      type: 'string',
      title: 'host',
      description: 'Host to connect to',
      format: 'hostname',
    },
    id: {
      type: 'string',
      title: 'Identifier',
      description: 'Unique identifier for this protocol adapter',
      minLength: 1,
      maxLength: 1024,
      format: 'identifier',
      pattern: '([a-zA-Z_0-9\\-])*',
    },
    pollingIntervalMillis: {
      type: 'integer',
      title: 'Polling interval [ms]',
      description: 'Interval in milliseconds to poll for changes',
      default: 10000,
      minimum: 100,
      maximum: 86400000,
    },
    port: {
      type: 'integer',
      title: 'Port',
      description: 'Port to connect to',
      minimum: 1,
      maximum: 65535,
    },
    subscriptions: {
      title: 'subscriptions',
      description: 'List of subscriptions for the simulation',
      type: 'array',
      items: {
        type: 'object',
        properties: {
          destination: {
            type: 'string',
            title: 'destination',
            description: 'The topic to publish data on',
            format: 'mqtt-topic',
          },
          filter: {
            type: 'string',
            title: 'filter',
            description: 'The local simulation filter topic',
            format: 'mqtt-topic',
          },
          qos: {
            type: 'integer',
            title: 'MQTT QoS',
            description: 'MQTT quality of service level',
            default: 0,
            minimum: 0,
            maximum: 2,
          },
        },
        required: ['destination', 'filter', 'qos'],
        title: 'subscriptions',
        description: 'List of subscriptions for the simulation',
      },
    },
  },
  required: ['host', 'id', 'port', 'subscriptions'],
}

export const mockProtocolAdapter: ProtocolAdapter = {
  id: 'simulation-adapter',
  protocol: 'Simulation Server',
  name: 'Simulation Server Protocol Adapter',
  description: 'Simulates traffic from an edge device.',
  url: 'https://www.hivemq.com/edge/simulation/',
  version: '1.0.0',
  logoUrl: 'https://www.hivemq.com/img/svg/hivemq-header-logo.svg',
  author: 'HiveMQ',
  configSchema: mockJSONSchema,
}

export const mockAdapterConfig: Record<string, Record<string, unknown>> = {}

export const mockAdapter: Adapter = {
  id: 'my-id',
  type: 'simulation-adapter',
  config: mockAdapterConfig,
}

export const handlers = [
  rest.get('*/protocol-adapters/types', (_, res, ctx) => {
    return res(ctx.json<ProtocolAdaptersList>({ items: [mockProtocolAdapter] }), ctx.status(200))
  }),

  rest.get('*/protocol-adapters/adapters', (_, res, ctx) => {
    return res(ctx.json<AdaptersList>({ items: [mockAdapter] }), ctx.status(200))
  }),

  rest.get('*/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    const { adapterType } = req.params
    return res(ctx.json<Adapter>({ ...mockAdapter, id: adapterType }), ctx.status(200))
  }),

  rest.post('*/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),

  rest.delete('*/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),

  rest.put('*/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),
]
