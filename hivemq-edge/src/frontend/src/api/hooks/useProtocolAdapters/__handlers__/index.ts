import { Adapter, AdaptersList, JsonNode, ProtocolAdapter, ProtocolAdaptersList, Status } from '@/api/__generated__'
import { rest } from 'msw'
import { MOCK_TOPIC_REF1, MOCK_TOPIC_REF2 } from '@/__test-utils__/react-flow/topics.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'

export const mockJSONSchema: JsonNode = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  type: 'object',
  properties: {
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
    subscriptions: {
      title: 'Subscriptions',
      description: 'List of subscriptions for the simulation',
      type: 'array',
      items: {
        type: 'object',
        properties: {
          destination: {
            type: 'string',
            title: 'Destination Topic',
            description: 'The topic to publish data on',
            format: 'mqtt-topic',
          },
          qos: {
            type: 'integer',
            title: 'QoS',
            description: 'MQTT quality of service level',
            default: 0,
            minimum: 0,
            maximum: 2,
          },
        },
        required: ['destination', 'qos'],
        title: 'Subscriptions',
        description: 'List of subscriptions for the simulation',
      },
    },
  },
  required: ['id', 'subscriptions'],
}

export const mockProtocolAdapter: ProtocolAdapter = {
  id: 'simulation',
  protocol: 'Simulation',
  name: 'Simulated Edge Device',
  description: 'Without needing to configure real devices, simulate traffic from an edge device into HiveMQ Edge.',
  url: 'https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#simulation-adapter',
  version: 'Development Snapshot',
  logoUrl: 'http://localhost:8080/images/hivemq-icon.png',
  author: 'HiveMQ',
  configSchema: mockJSONSchema,
  installed: true,
  category: {
    description: 'Industrial, typically field bus protocols.',
    displayName: 'Industrial',
    name: 'INDUSTRIAL',
  },
  tags: ['tag1', 'tag2', 'tag3'],
}

export const mockAdapterConfig: Record<string, Record<string, unknown>> = {
  id: MOCK_ADAPTER_ID,
  pollingIntervalMillis: 10000,
  subscriptions: [
    {
      destination: MOCK_TOPIC_REF1,
      qos: 0,
    },
    {
      destination: MOCK_TOPIC_REF2,
      qos: 0,
    },
  ],
} as never

export const mockAdapter: Adapter = {
  id: MOCK_ADAPTER_ID,
  type: 'simulation',
  config: mockAdapterConfig,
  status: {
    startedAt: '2023-08-21T11:51:24.234+01',
    connection: Status.connection.CONNECTED,
  },
}

export const handlers = [
  rest.get('**/protocol-adapters/types', (_, res, ctx) => {
    return res(ctx.json<ProtocolAdaptersList>({ items: [mockProtocolAdapter] }), ctx.status(200))
  }),

  rest.get('**/protocol-adapters/adapters', (_, res, ctx) => {
    return res(ctx.json<AdaptersList>({ items: [mockAdapter] }), ctx.status(200))
  }),

  rest.get('**/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    const { adapterType } = req.params
    return res(ctx.json<Adapter>({ ...mockAdapter, id: adapterType }), ctx.status(200))
  }),

  rest.post('**/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),

  rest.delete('**/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),

  rest.put('**/protocol-adapters/adapters/:adapterId/status', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),

  rest.put('**/protocol-adapters/adapters/:adapterType', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),
]
