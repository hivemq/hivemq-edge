import { http, HttpResponse } from 'msw'
import { UiSchema } from '@rjsf/utils'

import { MOCK_TOPIC_REF1, MOCK_TOPIC_REF2 } from '@/__test-utils__/react-flow/topics.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import {
  Adapter,
  AdaptersList,
  type DomainTag,
  type DomainTagList,
  JsonNode,
  ObjectNode,
  ProtocolAdapter,
  ProtocolAdaptersList,
  Status,
  type ValuesTree,
} from '@/api/__generated__'

export const mockUISchema: UiSchema = {
  'ui:tabs': [
    {
      id: 'coreFields',
      title: 'Settings',
      properties: ['id'],
    },
    {
      id: 'subFields',
      title: 'Subscription',
      properties: ['subscriptions'],
    },
    {
      id: 'publishing',
      title: 'Publishing',
      properties: ['maxPollingErrorsBeforeRemoval', 'pollingIntervalMillis', 'minValue', 'maxValue'],
    },
  ],
  'ui:submitButtonOptions': {
    norender: true,
  },
  id: {
    'ui:disabled': false,
  },
  subscriptions: {
    items: {
      'ui:order': ['destination', 'qos', '*'],
      'ui:collapsable': {
        titleKey: 'destination',
      },
    },
  },
}

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
  uiSchema: mockUISchema,
  installed: true,
  capabilities: ['READ', 'DISCOVER'],
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

export const mockDataPointOPCUA: ValuesTree = {
  items: [
    {
      id: 'i=85',
      name: 'Object',
      value: 'i=85',
      description:
        'The Apollotech B340 is an affordable wireless mouse with reliable connectivity, 12 months battery life and modern design',
      nodeType: ObjectNode.nodeType.FOLDER,
      selectable: false,
      children: [
        {
          id: 'ns=3;s=85/0:Simulation',
          name: 'Simulation',
          value: 'ns=3;s=85/0:Simulation',
          description:
            'Andy shoes are designed to keeping in mind durability as well as trends, the most stylish range of shoes & sandals',
          nodeType: ObjectNode.nodeType.FOLDER,
          selectable: false,
          children: [
            {
              id: 'ns=3;i=1001',
              name: 'Constant',
              value: 'ns=3;i=1001',
              description:
                'New range of formal shirts are designed keeping you in mind. With fits and styling that will make you stand apart',
              nodeType: ObjectNode.nodeType.VALUE,
              selectable: true,
            },
            {
              id: 'ns=3;i=1002',
              name: 'Counter',
              value: 'ns=3;i=1002',
              description: 'Carbonite web goalkeeper gloves are ergonomically designed to give easy fit',
              nodeType: ObjectNode.nodeType.VALUE,
              selectable: true,
            },
            {
              id: 'ns=3;i=1003',
              name: 'Random',
              value: 'ns=3;i=1003',
              description:
                'The automobile layout consists of a front-engine design, with transaxle-type transmissions mounted at the rear of the engine and four wheel drive',
              nodeType: ObjectNode.nodeType.VALUE,
              selectable: true,
            },
            {
              id: 'ns=3;i=1004',
              name: 'SawTooth',
              value: 'ns=3;i=1004',
              description:
                'Ergonomic executive chair upholstered in bonded black leather and PVC padded seat and back for all-day comfort and support',
              nodeType: ObjectNode.nodeType.VALUE,
              selectable: true,
            },
            {
              id: 'ns=3;i=1007',
              name: 'NewValues',
              value: 'ns=3;i=1007',
              description:
                'Andy shoes are designed to keeping in mind durability as well as trends, the most stylish range of shoes & sandals',
              nodeType: ObjectNode.nodeType.FOLDER,
              selectable: false,
              children: [
                {
                  id: 'ns=3;i=1010',
                  name: 'Triangle',
                  value: 'ns=3;i=1010',
                  description:
                    'The beautiful range of Apple Naturalé that has an exciting mix of natural ingredients. With the Goodness of 100% Natural Ingredients',
                  nodeType: ObjectNode.nodeType.VALUE,
                  selectable: true,
                },
                {
                  id: 'ns=3;i=1011',
                  name: 'Circle',
                  value: 'ns=3;i=1011',
                  description:
                    'New range of formal shirts are designed keeping you in mind. With fits and styling that will make you stand apart',
                  nodeType: ObjectNode.nodeType.VALUE,
                  selectable: true,
                },
              ],
            },
            {
              id: 'ns=3;i=1005',
              name: 'Sinusoid',
              value: 'ns=3;i=1005',
              description:
                'The beautiful range of Apple Naturalé that has an exciting mix of natural ingredients. With the Goodness of 100% Natural Ingredients',
              nodeType: ObjectNode.nodeType.VALUE,
              selectable: true,
            },
            {
              id: 'ns=3;i=1006',
              name: 'Square',
              value: 'ns=3;i=1006',
              description:
                'New range of formal shirts are designed keeping you in mind. With fits and styling that will make you stand apart',
              nodeType: ObjectNode.nodeType.VALUE,
              selectable: true,
            },
          ],
        },
      ],
    },
  ],
}

export const MOCK_DEVICE_TAGS = (adapterId: string): DomainTag[] => [
  { tag: `${adapterId}/power-management/alert`, dataPoint: { node: 'ns=3;i=1002' } },
  { tag: `${adapterId}/power-management/off`, dataPoint: { node: 'ns=3;i=1003' } },
  { tag: `${adapterId}/log/event`, dataPoint: { node: 'ns=3;i=1008' } },
]

export const handlers = [
  http.get('*/protocol-adapters/types', () => {
    return HttpResponse.json<ProtocolAdaptersList>({ items: [mockProtocolAdapter] }, { status: 200 })
  }),

  http.get('*/protocol-adapters/adapters', () => {
    return HttpResponse.json<AdaptersList>({ items: [mockAdapter] }, { status: 200 })
  }),

  http.get('*/protocol-adapters/adapters/:adapterType', ({ params }) => {
    const { adapterType } = params
    return HttpResponse.json<Adapter>({ ...mockAdapter, id: adapterType as string }, { status: 200 })
  }),

  http.post('*/protocol-adapters/adapters/:adapterType', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('*/protocol-adapters/adapters/:adapterType', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('**/protocol-adapters/adapters/:adapterId/status', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('**/protocol-adapters/adapters/:adapterType', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.get('*/protocol-adapters/adapters/:adapterId/discover', () => {
    return HttpResponse.json<ValuesTree>(mockDataPointOPCUA, { status: 200 })
  }),
]

export const deviceHandlers = [
  http.get('*/protocol-adapters/adapters/:adapterId/tags', ({ params }) => {
    const { adapterId } = params
    console.log('>', { adapterId })

    return HttpResponse.json<DomainTagList>({ items: MOCK_DEVICE_TAGS(adapterId as string) }, { status: 200 })
  }),

  http.post('*/protocol-adapters/adapters/:adapterId/tags', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('*/protocol-adapters/adapters/:adapterId/tags/:tagId', ({ params }) => {
    const { adapterId, tagId } = params
    console.log('>', { adapterId, tagId })

    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('*/protocol-adapters/adapters/:adapterId/tags/:tagId', ({ params }) => {
    const { adapterId, tagId } = params
    console.log('>', { adapterId, tagId })

    return HttpResponse.json({}, { status: 200 })
  }),
]
