import { GenericObjectType } from '@rjsf/utils'
import { ProtocolAdapter } from '@/api/__generated__'

export const MOCK_PROTOCOL_OPC_UA: ProtocolAdapter = {
  id: 'opc-ua-client',
  protocol: 'OPC-UA Client',
  name: 'OPC-UA to MQTT Protocol Adapter',
  description:
    'Connects HiveMQ Edge to existing OPC-UA services as a client and enables a seamless exchange of data between MQTT and OPC-UA.',
  url: 'https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#opc-ua-adapter',
  version: 'Development Snapshot',
  logoUrl: 'http://localhost:8080/images/opc-ua-icon.jpg',
  author: 'HiveMQ',
  installed: true,
  // capabilities: ['READ', 'DISCOVER'],
  category: {
    name: 'INDUSTRIAL',
    displayName: 'Industrial',
    description: 'Industrial, typically field bus protocols.',
  },
  tags: [],
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
        pattern: '([a-zA-Z_0-9\\-])*',
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
      subscriptions: {
        type: 'array',
        items: {
          type: 'object',
          properties: {
            'message-expiry-interval': {
              type: 'integer',
              title: 'MQTT message expiry interval [s]',
              description: 'Time in seconds until a MQTT message expires',
              minimum: 1,
              maximum: 4294967295,
            },
            'mqtt-topic': {
              type: 'string',
              title: 'Destination MQTT topic',
              description: 'The MQTT topic to publish to',
              format: 'mqtt-topic',
            },
            node: {
              type: 'string',
              title: 'Source Node ID',
              description: 'identifier of the node on the OPC-UA server. Example: "ns=3;s=85/0:Temperature"',
            },
            'publishing-interval': {
              type: 'integer',
              title: 'OPC UA publishing interval [ms]',
              description: 'OPC UA publishing interval in milliseconds for this subscription on the server',
              default: 1000,
              minimum: 1,
            },
            qos: {
              type: 'integer',
              title: 'MQTT QoS',
              description: 'MQTT quality of service level',
              default: 0,
              minimum: 0,
              maximum: 2,
            },
            'server-queue-size': {
              type: 'integer',
              title: 'OPC UA server queue size',
              description: 'OPC UA queue size for this subscription on the server',
              default: 1,
              minimum: 1,
            },
          },
          required: ['mqtt-topic', 'node'],
        },
      },
      tls: {
        type: 'object',
        properties: {
          enabled: {
            type: 'boolean',
            title: 'Enable TLS',
            description: 'Enables TLS encrypted connection',
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
              'private-key-password': {
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
        title: 'OPC-UA Server URI',
        description: 'URI of the OPC-UA server to connect to',
        format: 'uri',
      },
    },
    required: ['id', 'uri'],
  },
}

export const MOCK_ADAPTER_OPC_UA: GenericObjectType = {
  id: 'dd',
  type: 'opc-ua-client',
  config: {
    id: 'dd',
    uri: 'h:/ffgf',
    subscriptions: [
      {
        node: 'dd1',
        'mqtt-topic': 'a/valid/topic/opc-ua-client/1',
        'publishing-interval': 1000,
        'server-queue-size': 1,
        qos: 0,
      },
      {
        node: 'dd2',
        'mqtt-topic': 'a/valid/topic/opc-ua-client/2',
        'publishing-interval': 1000,
        'server-queue-size': 1,
        qos: 0,
      },
    ],
    auth: {},
    tls: {
      enabled: false,
    },
    security: {
      policy: 'NONE',
    },
  },
  adapterRuntimeInformation: {
    lastStartedAttemptTime: '2023-09-12T11:42:31.411+01',
    numberOfDaemonProcesses: 0,
    connectionStatus: {
      status: 'DISCONNECTED',
      id: 'dd',
      type: 'adapter',
    },
  },
}
