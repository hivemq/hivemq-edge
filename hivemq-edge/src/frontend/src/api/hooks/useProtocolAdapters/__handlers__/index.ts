import { Adapter, AdaptersList, JsonNode, ProtocolAdapter, ProtocolAdaptersList, Status } from '@/api/__generated__'
import { http, HttpResponse } from 'msw'
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
  url: 'https://docs.hivemq.com/hivemq-edge/index.html',
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
  http.get('**/protocol-adapters/types', () => {
    return HttpResponse.json<ProtocolAdaptersList>({ items: [mockProtocolAdapter] }, { status: 200 })
  }),

  http.get('**/protocol-adapters/adapters', () => {
    return HttpResponse.json<AdaptersList>({ items: [mockAdapter] }, { status: 200 })
  }),

  http.get('**/protocol-adapters/adapters/:adapterType', () => {
    // @ts-ignore
    const { adapterType } = req.params
    return HttpResponse.json<Adapter>({ ...mockAdapter, id: adapterType }, { status: 200 })
  }),

  http.post('**/protocol-adapters/adapters/:adapterType', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('**/protocol-adapters/adapters/:adapterType', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('**/protocol-adapters/adapters/:adapterId/status', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('**/protocol-adapters/adapters/:adapterType', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
