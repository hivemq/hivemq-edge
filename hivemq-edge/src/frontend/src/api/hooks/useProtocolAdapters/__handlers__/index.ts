import { http, HttpResponse } from 'msw'
import { GenericObjectType, UiSchema } from '@rjsf/utils'

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
      properties: ['id', 'minValue', 'maxValue', 'minDelay', 'maxDelay'],
    },
    {
      id: 'subFields',
      title: 'Simulation to MQTT',
      properties: ['simulationToMqtt'],
    },
  ],
  'ui:submitButtonOptions': {
    norender: true,
  },
  id: {
    'ui:disabled': false,
  },
  'ui:order': ['id', 'minValue', 'maxValue', 'minDelay', 'maxDelay', '*'],
  simulationToMqtt: {
    simulationToMqttMappings: {
      'ui:batchMode': true,
      items: {
        'ui:order': [
          'mqttTopic',
          'mqttQos',
          'mqttUserProperties',
          'messageHandlingOptions',
          'includeTimestamp',
          'includeTagNames',
          '*',
        ],
        'ui:collapsable': {
          titleKey: 'mqttTopic',
        },
      },
    },
  },
}

export const mockJSONSchema: JsonNode = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  type: 'object',
  properties: {
    minValue: {
      type: 'integer',
      title: 'Min. Generated Value',
      description: 'Minimum value of the generated decimal number',
      default: 0,
      minimum: 0,
    },
    maxValue: {
      type: 'integer',
      title: 'Max. Generated Value (Excl.)',
      description: 'Maximum value of the generated decimal number (excluded)',
      default: 1000,
      maximum: 1000,
    },
    simulationToMqtt: {
      type: 'object',
      properties: {
        maxPollingErrorsBeforeRemoval: {
          type: 'integer',
          title: 'Max. Polling Errors',
          description:
            'Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)',
          default: 10,
          minimum: -1,
        },
        pollingIntervalMillis: {
          type: 'integer',
          title: 'Polling Interval [ms]',
          description: 'Time in millisecond that this endpoint will be polled',
          default: 1000,
          minimum: 1,
        },
        simulationToMqttMappings: {
          title: 'simulationToMqttMappings',
          description: 'List of simulation to mqtt mappings for the simulation',
          type: 'array',
          items: {
            type: 'object',
            properties: {
              includeTagNames: {
                type: 'boolean',
                title: 'Include Tag Names In Publish?',
                description: 'Include the names of the tags in the resulting MQTT publish',
                default: false,
              },
              includeTimestamp: {
                type: 'boolean',
                title: 'Include Sample Timestamp In Publish?',
                description: 'Include the unix timestamp of the sample time in the resulting MQTT message',
                default: true,
              },
              messageHandlingOptions: {
                type: 'string',
                enum: ['MQTTMessagePerTag', 'MQTTMessagePerSubscription'],
                title: 'Message Handling Options',
                description:
                  'This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample',
                default: 'MQTTMessagePerTag',
                enumNames: [
                  'MQTT Message Per Device Tag',
                  'MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)',
                ],
              },
              mqttQos: {
                type: 'integer',
                title: 'MQTT QoS',
                description: 'MQTT Quality of Service level',
                default: 0,
                minimum: 0,
                maximum: 2,
              },
              mqttTopic: {
                type: 'string',
                title: 'Destination MQTT Topic',
                description: 'The topic to publish data on',
                format: 'mqtt-topic',
              },
              mqttUserProperties: {
                title: 'MQTT User Properties',
                description: 'Arbitrary properties to associate with the mapping',
                maxItems: 10,
                type: 'array',
                items: {
                  type: 'object',
                  properties: {
                    name: {
                      type: 'string',
                      title: 'Name',
                      description: 'Name of the associated property',
                    },
                    value: {
                      type: 'string',
                      title: 'Value',
                      description: 'Value of the associated property',
                    },
                  },
                  required: ['name', 'value'],
                  maxItems: 10,
                },
              },
            },
            required: ['mqttTopic'],
          },
        },
      },
      title: 'simulationToMqtt',
      description: 'Define Simulations to create MQTT messages.',
    },
    id: {
      type: 'string',
      title: 'Identifier',
      description: 'Unique identifier for this protocol adapter',
      minLength: 1,
      maxLength: 1024,
      format: 'identifier',
      pattern: '^([a-zA-Z_0-9-_])*$',
    },
    minDelay: {
      type: 'integer',
      title: 'Minimum of delay',
      description: 'Minimum of artificial delay before the polling method generates a value',
      default: 0,
      minimum: 0,
    },
    maxDelay: {
      type: 'integer',
      title: 'Maximum of delay',
      description: 'Maximum of artificial delay before the polling method generates a value',
      default: 0,
      minimum: 0,
    },
  },
  required: ['simulationToMqtt', 'id'],
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

export const mockAdapterConfig: GenericObjectType = {
  minValue: 0,
  maxValue: 1000,
  simulationToMqtt: {
    pollingIntervalMillis: 10000,
    maxPollingErrorsBeforeRemoval: 10,
    simulationToMqttMappings: [
      {
        mqttTopic: MOCK_TOPIC_REF1,
        qos: 0,
      },
      {
        mqttTopic: MOCK_TOPIC_REF2,
        qos: 0,
      },
    ],
  },
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

    return HttpResponse.json<DomainTagList>({ items: MOCK_DEVICE_TAGS(adapterId as string) }, { status: 200 })
  }),

  http.post('*/protocol-adapters/adapters/:adapterId/tags', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('*/protocol-adapters/adapters/:adapterId/tags/:tagId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('*/protocol-adapters/adapters/:adapterId/tags/:tagId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
