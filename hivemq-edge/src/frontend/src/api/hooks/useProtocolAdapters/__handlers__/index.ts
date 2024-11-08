import { http, HttpResponse } from 'msw'
import { GenericObjectType, UiSchema } from '@rjsf/utils'
import { JSONSchema7 } from 'json-schema'

import { MOCK_TOPIC_REF1, MOCK_TOPIC_REF2 } from '@/__test-utils__/react-flow/topics.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import {
  Adapter,
  AdaptersList,
  type DeviceDataPoint,
  type DomainTag,
  type DomainTagList,
  ObjectNode,
  ProtocolAdapter,
  ProtocolAdaptersList,
  Status,
  type ValuesTree,
} from '@/api/__generated__'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

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
  id: {
    'ui:disabled': false,
  },
  'ui:order': ['id', 'minValue', 'maxValue', 'minDelay', 'maxDelay', '*'],
  simulationToMqtt: {
    'ui:order': ['simulationToMqttMappings', 'pollingIntervalMillis', 'maxPollingErrorsBeforeRemoval', '*'],
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

export const mockJSONSchema: JSONSchema7 = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  type: 'object',
  $defs: {
    minValue: {
      type: 'integer',
      title: 'Min. Generated Value',
      description: 'Minimum value of the generated decimal number',
      default: 0,
      minimum: 0,
    },
  },
  definitions: {
    maxValue: {
      type: 'integer',
      title: 'Max. Generated Value (Excl.)',
      description: 'Maximum value of the generated decimal number (excluded)',
      default: 1000,
      maximum: 1000,
    },
  },
  properties: {
    minValue: {
      $ref: '#/$defs/minValue',
    },
    maxValue: {
      $ref: '#/definitions/maxValue',
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
                // @ts-ignore TODO[NVL] enumNames not officially supported
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
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#simulation-adapter',
  version: 'Development Version',
  logoUrl: '/module/images/hivemq-icon.png',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['READ'],
  category: {
    name: 'SIMULATION',
    displayName: 'Simulation',
    description: 'Simulation protocols, that emulate real world devices',
  },
  tags: ['tag1', 'tag2', 'tag3'],
  configSchema: mockJSONSchema,
  uiSchema: mockUISchema,
}

export const mockProtocolAdapter_OPCUA: ProtocolAdapter = {
  id: 'opcua',
  protocol: 'OPC UA',
  name: 'OPC UA to MQTT Protocol Adapter',
  description:
    'Connects HiveMQ Edge to existing OPC UA services as a client and enables a seamless exchange of data between MQTT and OPC-UA.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#opc-ua-adapter',
  version: 'Development Version',
  logoUrl: '/module/images/opc-ua-icon.jpg',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['READ', 'DISCOVER', 'WRITE'],
  category: {
    name: 'INDUSTRIAL',
    displayName: 'Industrial',
    description: 'Industrial, typically field bus protocols.',
  },
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      auth: {
        type: 'object',
        properties: {
          basic: {
            type: 'object',
            properties: {
              password: {
                type: 'string',
                title: 'Password',
                description: 'Password for basic authentication',
              },
              username: {
                type: 'string',
                title: 'Username',
                description: 'Username for basic authentication',
              },
            },
            title: 'Basic Authentication',
            description: 'Username / password based authentication',
          },
          x509: {
            type: 'object',
            properties: {
              enabled: {
                type: 'boolean',
                title: 'Enable X509',
                description: 'Enables X509 auth',
              },
            },
            title: 'X509 Authentication',
            description: 'Authentication based on certificate / private key',
          },
        },
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
      mqttToOpcua: {
        type: 'object',
        properties: {
          mqttToOpcuaMappings: {
            title: 'mqttToOpcuaMappings',
            description: 'Map your MQTT data to OpcUA.',
            type: 'array',
            items: {
              type: 'object',
              properties: {
                mqttMaxQos: {
                  type: 'integer',
                  title: 'MQTT Maximum QoS',
                  description: 'MQTT maximum quality of service level for the subscription',
                  default: 1,
                  minimum: 0,
                  maximum: 1,
                },
                mqttTopicFilter: {
                  type: 'string',
                  title: 'Source MQTT topic filter',
                  description: 'The MQTT topic filter to map from',
                  format: 'mqtt-topic-filter',
                },
                node: {
                  type: 'string',
                  title: 'Destination Node ID',
                  description: 'identifier of the node on the OPC UA server. Example: "ns=3;s=85/0:Temperature"',
                },
              },
              required: ['mqttTopicFilter', 'node'],
            },
          },
        },
        title: 'Mqtt to OpcUA Config',
        description: 'The configuration for a data stream from MQTT to OpcUa',
      },
      opcuaToMqtt: {
        type: 'object',
        properties: {
          opcuaToMqttMappings: {
            title: 'opcuaToMqttMappings',
            description: 'Map your sensor data to MQTT Topics',
            type: 'array',
            items: {
              type: 'object',
              properties: {
                messageExpiryInterval: {
                  type: 'integer',
                  title: 'MQTT message expiry interval [s]',
                  description: 'Time in seconds until an MQTT publish message expires',
                  minimum: 1,
                  maximum: 4294967295,
                },
                mqttQos: {
                  type: 'integer',
                  title: 'MQTT QoS',
                  description: 'MQTT quality of service level',
                  default: 0,
                  minimum: 0,
                  maximum: 2,
                },
                mqttTopic: {
                  type: 'string',
                  title: 'Destination MQTT topic',
                  description: 'The MQTT topic to publish to',
                  format: 'mqtt-topic',
                },
                node: {
                  type: 'string',
                  title: 'Source Node ID',
                  description: 'identifier of the node on the OPC UA server. Example: "ns=3;s=85/0:Temperature"',
                },
                publishingInterval: {
                  type: 'integer',
                  title: 'OPC UA publishing interval [ms]',
                  description: 'OPC UA publishing interval in milliseconds for this subscription on the server',
                  default: 1000,
                  minimum: 1,
                },
                serverQueueSize: {
                  type: 'integer',
                  title: 'OPC UA server queue size',
                  description: 'OPC UA queue size for this subscription on the server',
                  default: 1,
                  minimum: 1,
                },
              },
              required: ['mqttTopic', 'node'],
            },
          },
        },
        title: 'OpcUA To MQTT Config',
        description: 'The configuration for a data stream from OpcUA to MQTT',
      },
      overrideUri: {
        type: 'boolean',
        title: 'Override server returned endpoint URI',
        description:
          'Overrides the endpoint URI returned from the OPC UA server with the hostname and port from the specified URI.',
        default: false,
        format: 'boolean',
      },
      security: {
        type: 'object',
        properties: {
          policy: {
            type: 'string',
            enum: [
              'NONE',
              'BASIC128RSA15',
              'BASIC256',
              'BASIC256SHA256',
              'AES128_SHA256_RSAOAEP',
              'AES256_SHA256_RSAPSS',
            ],
            title: 'OPC UA security policy',
            description: 'Security policy to use for communication with the server.',
          },
        },
      },
      tls: {
        type: 'object',
        properties: {
          enabled: {
            type: 'boolean',
            title: 'Enable TLS',
            description: 'Enables TLS encrypted connection',
            default: true,
          },
          keystore: {
            type: 'object',
            properties: {
              password: {
                type: 'string',
                title: 'Keystore password',
                description: 'Password to open the keystore.',
              },
              path: {
                type: 'string',
                title: 'Keystore path',
                description: 'Path on the local file system to the keystore.',
              },
              privateKeyPassword: {
                type: 'string',
                title: 'Private key password',
                description: 'Password to access the private key.',
              },
            },
            title: 'Keystore',
            description:
              'Keystore that contains the client certificate including the chain. Required for X509 authentication.',
          },
          truststore: {
            type: 'object',
            properties: {
              password: {
                type: 'string',
                title: 'Truststore password',
                description: 'Password to open the truststore.',
              },
              path: {
                type: 'string',
                title: 'Truststore path',
                description: 'Path on the local file system to the truststore.',
              },
            },
            title: 'Truststore',
            description: 'Truststore wich contains the trusted server certificates or trusted intermediates.',
          },
        },
      },
      uri: {
        type: 'string',
        title: 'OPC UA Server URI',
        description: 'URI of the OPC UA server to connect to',
        format: 'uri',
      },
    },
    required: ['id', 'uri'],
  },
  uiSchema: {
    'ui:tabs': [
      {
        id: 'coreFields',
        title: 'Connection',
        properties: ['id', 'uri', 'overrideUri', 'security', 'tls', 'auth'],
      },
      {
        id: 'opcuaToMqtt',
        title: 'OPC UA to MQTT',
        properties: ['opcuaToMqtt'],
      },
      {
        id: 'mqttToOpcua',
        title: 'MQTT to OPC UA',
        properties: ['mqttToOpcua'],
      },
    ],
    id: {
      'ui:disabled': true,
    },
    'ui:order': ['id', 'uri', 'overrideUri', 'security', 'tls', 'auth', '*'],
    opcuaToMqtt: {
      'ui:batchMode': true,
      opcuaToMqttMappings: {
        items: {
          'ui:order': ['node', 'mqttTopic', 'mqttQos', '*'],
          'ui:collapsable': {
            titleKey: 'mqttTopic',
          },
          node: {
            'ui:widget': 'discovery:tagBrowser',
          },
        },
      },
    },
    mqttToOpcua: {
      'ui:batchMode': true,
      mqttToOpcuaMappings: {
        items: {
          'ui:order': ['node', 'mqttTopicFilter', 'mqttMaxQos', '*'],
          'ui:collapsable': {
            titleKey: 'mqttTopicFilter',
          },
          node: {
            'ui:widget': 'discovery:tagBrowser',
          },
        },
      },
    },
    auth: {
      basic: {
        'ui:order': ['username', 'password', '*'],
      },
    },
  },
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

export const mockAdapter_OPCUA: Adapter = {
  id: 'opcua-1',
  type: 'opcua',
  config: {
    // @ts-ignore TODO[26764] bug with backend, https://hivemq.kanbanize.com/ctrl_board/57/cards/26764/details/
    id: 'opcua-1',
    // @ts-ignore TODO[26764] bug with backend, https://hivemq.kanbanize.com/ctrl_board/57/cards/26764/details/
    uri: 'opc.tcp://test.host.local:53530/OPCUA/Server',
    // @ts-ignore TODO[26764] bug with backend, https://hivemq.kanbanize.com/ctrl_board/57/cards/26764/details/
    overrideUri: false,
    tls: {
      enabled: false,
    },
    mqttToOpcua: {},
    opcuaToMqtt: {
      opcuaToMqttMappings: [
        {
          node: 'ns=3;s=85/0:Temperature',
          mqttTopic: 'generator/group1/energy',
          publishingInterval: 1000,
          serverQueueSize: 1,
          mqttQos: 0,
          messageExpiryInterval: 4294967295,
        },
      ],
    },
    security: {
      policy: 'NONE',
    },
  },
  status: {
    runtime: Status.runtime.STOPPED,
    connection: Status.connection.ERROR,
    id: 'opcua-1',
    type: 'adapter',
    startedAt: '2024-10-08T10:34:21.692+01',
    message:
      "com.hivemq.edge.adapters.opcua.OpcUaException: OPC UA subscription failed for nodeId `NodeId{ns=3, id=85/0:Temperature}`: Bad_NodeIdUnknown,The node id refers to a node that does not exist in the server address space. (status 'StatusCode{name=Bad_NodeIdUnknown, value=0x80340000, quality=bad}')",
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

export const MOCK_DEVICE_TAG_ADDRESS_MODBUS: DeviceDataPoint = { startIdx: 0, endIdx: 1 }
export const MOCK_DEVICE_TAG_ADDRESS_OPCUA: DeviceDataPoint = { node: 'ns=3;i=1002' }

export const MOCK_DEVICE_TAGS = (adapterId: string, type?: string | null): DomainTag[] => {
  switch (type) {
    case MockAdapterType.MODBUS:
      return [{ tag: `${adapterId}/alert`, dataPoint: MOCK_DEVICE_TAG_ADDRESS_MODBUS }]
    case MockAdapterType.OPC_UA:
      return [
        { tag: `${adapterId}/power/off`, dataPoint: MOCK_DEVICE_TAG_ADDRESS_OPCUA },
        { tag: `${adapterId}/log/event`, dataPoint: { node: 'ns=3;i=1008' } },
      ]
    default:
      return [{ tag: `${adapterId}/log/event`, dataPoint: {} }]
  }
}

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
  http.get('*/protocol-adapters/adapters/:adapterId/tags', ({ params, request }) => {
    const { adapterId } = params
    const url = new URL(request.url)
    const type = url.searchParams.get('type')

    return HttpResponse.json<DomainTagList>({ items: MOCK_DEVICE_TAGS(adapterId as string, type) }, { status: 200 })
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

  http.put('*/protocol-adapters/adapters/:adapterId/tags', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
