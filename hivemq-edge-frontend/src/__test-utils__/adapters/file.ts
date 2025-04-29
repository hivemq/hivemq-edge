/* generated from Edge version 2025.5 -- do no edit */
import type { Adapter, ProtocolAdapter, TagSchema } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const MOCK_PROTOCOL_FILE: ProtocolAdapter = {
  id: 'file',
  protocol: 'File Protocol',
  name: 'File Adapter',
  description: 'This adapter polls and publishes the content of files on regular basis.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html',
  version: 'Development Version',
  logoUrl: '/module/images/file.png',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['READ'],
  category: {
    name: 'CONNECTIVITY',
    displayName: 'Connectivity',
    description: 'A standard connectivity based protocol, typically web standard.',
  },
  tags: ['IOT', 'IIOT'],
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      fileToMqtt: {
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
        },
        title: 'File To MQTT Config',
        description: 'The configuration for a data stream from File to MQTT',
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
    },
    required: ['fileToMqtt', 'id'],
  },
  uiSchema: {
    'ui:tabs': [
      {
        id: 'coreFields',
        title: 'Connection',
        properties: ['id', '*'],
      },
      {
        id: 'subFields',
        title: 'File to MQTT',
        properties: ['fileToMqtt'],
      },
    ],
    id: {
      'ui:disabled': false,
    },
    'ui:order': ['id', '*'],
    fileToMqtt: {
      'ui:batchMode': true,
      'ui:order': ['fileToMqttMappings', 'maxPollingErrorsBeforeRemoval', 'pollingIntervalMillis', '*'],
      fileToMqttMappings: {
        'ui:batchMode': true,
        items: {
          'ui:order': ['mqttTopic', 'filePath', 'contentType', 'mqttQos', '*', 'mqttUserProperties'],
          'ui:collapsable': {
            titleKey: 'mqttTopic',
          },
        },
      },
    },
  },
}

export const MOCK_ADAPTER_FILE: Adapter = {
  id: 'test-file',
  config: {
    fileToMqtt: {
      pollingIntervalMillis: 1000,
      maxPollingErrorsBeforeRemoval: 10,
    },
    id: 'test-file',
  },
  status: {
    connection: Status.connection.STATELESS,
    id: 'test-file',
    runtime: Status.runtime.STARTED,
    startedAt: '2025-04-07T21:39:49.82Z',
    type: 'adapter',
  },
  type: 'file',
}

export const MOCK_SCHEMA_FILE: TagSchema = {
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      definition: {
        type: 'object',
        properties: {
          httpHeaders: {
            title: 'HTTP Headers',
            description: 'HTTP headers to be added to your requests',
            type: 'array',
            items: {
              type: 'object',
              properties: {
                name: {
                  type: 'string',
                  title: 'Name',
                  description: 'The name of the HTTP header',
                },
                value: {
                  type: 'string',
                  title: 'Value',
                  description: 'The value of the HTTP header',
                },
              },
              required: ['name', 'value'],
            },
          },
          httpRequestBody: {
            type: 'string',
            title: 'Http Request Body',
            description: 'The body to include in the HTTP request',
          },
          httpRequestBodyContentType: {
            type: 'string',
            enum: ['JSON', 'PLAIN', 'HTML', 'XML', 'YAML'],
            title: 'Http Request Content Type',
            description: 'Content Type associated with the request',
            default: 'JSON',
          },
          httpRequestMethod: {
            type: 'string',
            enum: ['GET', 'POST', 'PUT'],
            title: 'Http Method',
            description: 'Http method associated with the request',
            default: 'GET',
          },
          httpRequestTimeoutSeconds: {
            type: 'integer',
            title: 'Http Request Timeout',
            description: 'Timeout (in seconds) to wait for the HTTP Request to complete',
            default: 5,
            minimum: 1,
            maximum: 60,
          },
          url: {
            type: 'string',
            title: 'URL',
            description: 'The url of the HTTP request you would like to make',
            format: 'uri',
          },
        },
        required: ['url'],
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
  protocolId: 'http',
}
