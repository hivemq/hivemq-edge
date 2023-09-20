import { GenericObjectType } from '@rjsf/utils'
import { ProtocolAdapter } from '@/api/__generated__'

export const MOCK_PROTOCOL_MODBUS: ProtocolAdapter = {
  id: 'modbus',
  protocol: 'Modbus TCP',
  name: 'Modbus to MQTT Protocol Adapter',
  description: 'Connects HiveMQ Edge to existing Modbus devices, bringing data from coils & registers into MQTT.',
  url: 'https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#modbus-tcp-adapter',
  version: 'Development Snapshot',
  logoUrl: 'http://localhost:8080/images/modbus-icon.png',
  author: 'HiveMQ',
  installed: true,
  // capabilities: ['READ', 'DISCOVER'],
  category: {
    name: 'INDUSTRIAL',
    displayName: 'Industrial',
    description: 'Industrial, typically field bus protocols.',
  },
  tags: ['TCP'],
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      host: {
        type: 'string',
        title: 'Host',
        description: 'IP Address or hostname of the device you wish to connect to',
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
      maxPollingErrorsBeforeRemoval: {
        type: 'integer',
        title: 'Max. Polling Errors',
        description: 'Max. errors polling the endpoint before the polling daemon is stopped',
        default: 10,
        minimum: 3,
      },
      port: {
        type: 'integer',
        title: 'Port',
        description: 'The port number on the device you wish to connect to',
        minimum: 1,
        maximum: 65535,
      },
      publishChangedDataOnly: {
        type: 'boolean',
        title: 'Only publish data items that have changed since last poll',
        default: true,
        format: 'boolean',
      },
      publishingInterval: {
        type: 'integer',
        title: 'Publishing interval [ms]',
        description: 'Publishing interval in milliseconds for this subscription on the server',
        default: 1000,
        minimum: 1,
      },
      subscriptions: {
        title: 'Subscriptions',
        description: 'Map your sensor data to MQTT Topics',
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
            'holding-registers': {
              type: 'object',
              properties: {
                startIdx: {
                  type: 'integer',
                  title: 'Start Index',
                  description: 'The Starting Index (Incl.) of the Address Range',
                  default: 0,
                  minimum: 0,
                  maximum: 65535,
                },
                endIdx: {
                  type: 'integer',
                  title: 'End Index',
                  description: 'The Finishing Index (Incl.) of the Address Range',
                  default: 1,
                  minimum: 1,
                  maximum: 65535,
                },
              },
              title: 'Holding Registers',
              description: 'Define the start and end index values for your holding registers',
            },
            qos: {
              type: 'integer',
              title: 'QoS',
              description: 'MQTT Quality of Service level',
              default: 0,
              minimum: 0,
              maximum: 2,
            },
          },
          required: ['destination', 'qos'],
          title: 'Subscriptions',
          description: 'Map your sensor data to MQTT Topics',
        },
      },
    },
    required: ['host', 'id', 'port', 'publishingInterval'],
  },
}

export const MOCK_ADAPTER_MODBUS: GenericObjectType = {
  id: 'fg',
  type: 'modbus',
  config: {
    id: 'fg',
    port: 3,
    host: 'cvvccvvb.bvc.vb.vb.vb.bvbv',
    publishingInterval: 1000,
    maxPollingErrorsBeforeRemoval: 10,
    publishChangedDataOnly: true,
    subscriptions: [
      {
        destination: 'a/valid/topic/modbus/1',
        qos: 0,
        'holding-registers': {
          startIdx: 0,
          endIdx: 1,
        },
      },
    ],
  },
  adapterRuntimeInformation: {
    lastStartedAttemptTime: '2023-09-12T11:41:45.534+01',
    numberOfDaemonProcesses: 0,
    connectionStatus: {
      status: 'DISCONNECTED',
      id: 'fg',
      type: 'adapter',
    },
  },
}
