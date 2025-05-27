/* generated from Edge version 2025.5 -- do no edit */
import type { Adapter, ProtocolAdapter, TagSchema } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const MOCK_PROTOCOL_MODBUS: ProtocolAdapter = {
  id: 'modbus',
  protocol: 'Modbus TCP',
  name: 'Modbus Protocol Adapter',
  description: 'Connects HiveMQ Edge to existing Modbus devices.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#modbus-tcp-adapter',
  version: 'Development Version',
  logoUrl: '/module/images/modbus-icon.png',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['DISCOVER', 'READ'],
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
        writeOnly: true,
        minLength: 1,
        maxLength: 1024,
        format: 'identifier',
        pattern: '^([a-zA-Z_0-9-_])*$',
      },
      modbusToMqtt: {
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
        title: 'Modbus To MQTT Config',
        description: 'The configuration for a data stream from Modbus to MQTT',
      },
      port: {
        type: 'integer',
        title: 'Port',
        description: 'The port number on the device you wish to connect to',
        minimum: 1,
        maximum: 65535,
      },
      timeoutMillis: {
        type: 'integer',
        title: 'Timeout',
        description: 'Time (in milliseconds) to await a connection before the client gives up',
        default: 5000,
        minimum: 1000,
        maximum: 15000,
      },
    },
    required: ['host', 'id', 'modbusToMqtt', 'port'],
  },
  uiSchema: {
    'ui:tabs': [
      {
        id: 'coreFields',
        title: 'Connection',
        properties: ['id', 'port', 'host', 'timeoutMillis'],
      },
      {
        id: 'subFields',
        title: 'Modbus To MQTT',
        properties: ['modbusToMqtt'],
      },
    ],
    id: {
      'ui:disabled': false,
    },
    port: {
      'ui:widget': 'updown',
    },
    'ui:order': ['id', 'host', 'port', '*'],
    modbusToMqtt: {
      'ui:batchMode': true,
      'ui:order': [
        'modbusToMqttMappings',
        'maxPollingErrorsBeforeRemoval',
        'pollingIntervalMillis',
        'publishChangedDataOnly',
        '*',
      ],
      modbusToMqttMappings: {
        'ui:batchMode': true,
        items: {
          'ui:order': ['mqttTopic', 'mqttQos', 'addressRange', '*', 'mqttUserProperties'],
          'ui:collapsable': {
            titleKey: 'mqttTopic',
          },
          addressRange: {
            endIdx: {
              'ui:widget': 'discovery:tagBrowser',
            },
            startIdx: {
              'ui:widget': 'discovery:tagBrowser',
            },
          },
        },
      },
    },
  },
}

export const MOCK_ADAPTER_MODBUS: Adapter = {
  id: 'test-modbus',
  config: {
    port: 255,
    host: 'test.co.jp',
    timeoutMillis: 5000,
    modbusToMqtt: {
      pollingIntervalMillis: 1000,
      maxPollingErrorsBeforeRemoval: 10,
      publishChangedDataOnly: true,
    },
    id: 'test-modbus',
  },
  status: {
    connection: Status.connection.DISCONNECTED,
    id: 'test-modbus',
    runtime: Status.runtime.STOPPED,
    startedAt: '2025-04-07T21:22:57.293Z',
    type: 'adapter',
  },
  type: 'modbus',
}

export const MOCK_SCHEMA_MODBUS: TagSchema = {
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      definition: {
        type: 'object',
        properties: {
          dataType: {
            type: 'string',
            enum: ['BOOL', 'INT_16', 'UINT_16', 'INT_32', 'UINT_32', 'INT_64', 'FLOAT_32', 'FLOAT_64', 'UTF_8'],
            title: 'Data Type',
            description: 'Define how the read registers are interpreted',
            default: 'INT_16',
          },
          flipRegisters: {
            type: 'boolean',
            title: 'Indicates if registers should be evaluated in reverse order',
            description:
              'Registers and their contents are normally written/read as big endian, some implementations decided to write the content as big endian but to order the actual registers as little endian.',
            default: false,
          },
          readType: {
            type: 'string',
            enum: ['COILS', 'DISCRETE_INPUTS', 'INPUT_REGISTERS', 'HOLDING_REGISTERS'],
            title: 'The way the register range should be read',
            description: 'Type of read to performe on the registers',
          },
          startIdx: {
            type: 'integer',
            title: 'Start Index',
            description: 'The Starting Index (Incl.) of the Address Range',
            minimum: 0,
            maximum: 65535,
          },
          unitId: {
            type: 'integer',
            title: 'The id of the unit to access',
            description: 'Id of the unit to access on the modbus',
          },
        },
        required: ['readType', 'startIdx', 'unitId'],
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
  protocolId: 'modbus',
}
