/* generated from Edge version 2025.5 -- do no edit */
import type { Adapter, ProtocolAdapter, TagSchema } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const MOCK_PROTOCOL_OPC_UA: ProtocolAdapter = {
  id: 'opcua',
  protocol: 'OPC UA',
  name: 'OPC UA Protocol Adapter',
  description: 'Supports Northbound and Southbound communicates from and to OPC UA.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#opc-ua-adapter',
  version: 'Development Version',
  logoUrl: '/module/images/opc-ua-icon.jpg',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['DISCOVER', 'WRITE', 'READ', 'COMBINE'],
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
        writeOnly: true,
        minLength: 1,
        maxLength: 1024,
        format: 'identifier',
        pattern: '^([a-zA-Z_0-9-_])*$',
      },
      opcuaToMqtt: {
        type: 'object',
        properties: {
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
        title: 'OPC UA To MQTT Config',
        description: 'The configuration for a data stream from OPC UA to MQTT',
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
            default: 'NONE',
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
            default: false,
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

export const MOCK_ADAPTER_OPC_UA: Adapter = {
  id: 'opcua-pump',
  config: {
    uri: 'opc.tcp://Nicolass-MacBook-Pro.local:53530/OPCUA/SimulationServer',
    overrideUri: false,
    tls: {
      enabled: false,
    },
    opcuaToMqtt: {
      publishingInterval: 1000,
      serverQueueSize: 1,
    },
    security: {
      policy: 'NONE',
    },
    id: 'opcua-pump',
  },
  status: {
    connection: Status.connection.CONNECTED,
    id: 'opcua-pump',
    runtime: Status.runtime.STARTED,
    startedAt: '2025-04-05T20:41:00.293Z',
    type: 'adapter',
  },
  type: 'opcua',
}

export const MOCK_SCHEMA_OPC_UA: TagSchema = {
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      definition: {
        type: 'object',
        properties: {
          node: {
            type: 'string',
            title: 'Destination Node ID',
            description: 'identifier of the node on the OPC UA server. Example: "ns=3;s=85/0:Temperature"',
          },
        },
        required: ['node'],
        title: 'definition',
        description: 'The actual definition of the tag on the device',
      },
      description: {
        type: 'string',
        title: 'description',
        description: 'A human readable description of the tag',
      },
      name: {
        type: 'string',
        title: 'name',
        description: 'name of the tag to be used in mappings',
        format: 'mqtt-tag',
      },
    },
    required: ['definition', 'name'],
  },
  protocolId: 'opcua',
}
