/* generated from Edge version 2025.5 -- do no edit */
import type { Adapter, ProtocolAdapter, TagSchema } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const MOCK_PROTOCOL_EIP: ProtocolAdapter = {
  id: 'eip',
  protocol: 'Ethernet/IP CIP',
  name: 'Ethernet IP Protocol Adapter',
  description:
    'Connects HiveMQ Edge to Rockwell / Allen-Bradley ControlLogix and CompactLogix devices supporting Ethernet IP.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#eip-adapter',
  version: 'Development Version',
  logoUrl: '/module/images/ab-eth-icon.png',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['READ'],
  category: {
    name: 'INDUSTRIAL',
    displayName: 'Industrial',
    description: 'Industrial, typically field bus protocols.',
  },
  tags: ['TCP', 'AUTOMATION', 'IIOT', 'FACTORY'],
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      backplane: {
        type: 'integer',
        title: 'Backplane',
        description: 'Backplane device value',
        default: 1,
      },
      eipToMqtt: {
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
          publishChangedDataOnly: {
            type: 'boolean',
            title: 'Only publish data items that have changed since last poll',
            default: true,
            format: 'boolean',
          },
        },
        title: 'Ethernet IP To MQTT Config',
        description: 'The configuration for a data stream from Ethernet IP to MQTT',
      },
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
        writeOnly: true,
        minLength: 1,
        maxLength: 1024,
        format: 'identifier',
        pattern: '^([a-zA-Z_0-9-_])*$',
      },
      port: {
        type: 'integer',
        title: 'Port',
        description: 'The port number on the device you wish to connect to',
        default: 44818,
        minimum: 1,
        maximum: 65535,
      },
      slot: {
        type: 'integer',
        title: 'Slot',
        description: 'Slot device value',
        default: 0,
      },
    },
    required: ['eipToMqtt', 'host', 'id', 'port'],
  },
  uiSchema: {
    'ui:tabs': [
      {
        id: 'coreFields',
        title: 'Connection',
        properties: ['id', 'port', 'host'],
      },
      {
        id: 'subFields',
        title: 'EIP to MQTT',
        properties: ['eipToMqtt'],
      },
      {
        id: 'eip',
        title: 'EIP Device',
        properties: ['slot', 'backplane'],
      },
    ],
    id: {
      'ui:disabled': false,
    },
    port: {
      'ui:widget': 'updown',
    },
    'ui:order': ['id', 'host', 'port', '*'],
    eipToMqtt: {
      'ui:batchMode': true,
      'ui:order': [
        'eipToMqttMappings',
        'maxPollingErrorsBeforeRemoval',
        'pollingIntervalMillis',
        'publishChangedDataOnly',
        '*',
      ],
      eipToMqttMappings: {
        'ui:batchMode': true,
        items: {
          'ui:order': ['tagName', 'tagAddress', 'dataType', 'mqttTopic', 'mqttQos', '*'],
          'ui:collapsable': {
            titleKey: 'mqttTopic',
          },
        },
      },
    },
  },
}

export const MOCK_ADAPTER_EIP: Adapter = {
  id: 'test-eip',
  config: {
    port: 44818,
    host: 'test.jp',
    backplane: 1,
    slot: 0,
    eipToMqtt: {
      pollingIntervalMillis: 1000,
      maxPollingErrorsBeforeRemoval: 10,
      publishChangedDataOnly: true,
    },
    id: 'test-eip',
  },
  status: {
    connection: Status.connection.DISCONNECTED,
    id: 'test-eip',
    runtime: Status.runtime.STOPPED,
    startedAt: '2025-04-07T21:42:39.04Z',
    type: 'adapter',
  },
  type: 'eip',
}

export const MOCK_SCHEMA_EIP: TagSchema = {
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      definition: {
        type: 'object',
        properties: {
          address: {
            type: 'string',
            title: 'address',
            description: 'Address of the tag on the device',
          },
          dataType: {
            type: 'string',
            enum: [
              'BOOL',
              'DINT',
              'INT',
              'LINT',
              'LREAL',
              'LTIME',
              'REAL',
              'SINT',
              'STRING',
              'TIME',
              'UDINT',
              'UINT',
              'ULINT',
              'USINT',
            ],
            title: 'Data Type',
            description: 'The expected data type of the tag',
            enumNames: [
              'Bool',
              'DInt',
              'Int',
              'LInt',
              'LReal',
              'LTime',
              'Real',
              'SInt',
              'String',
              'Time',
              'UDInt',
              'UInt',
              'ULInt',
              'USInt',
            ],
          },
        },
        required: ['address', 'dataType'],
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
    required: ['definition', 'description', 'name'],
  },
  protocolId: 'eip',
}
