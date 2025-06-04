/* generated from Edge version 2025.5 -- do no edit */
import type { Adapter, ProtocolAdapter, TagSchema } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const MOCK_PROTOCOL_HTTP: ProtocolAdapter = {
  id: 'http',
  protocol: 'HTTP(s) over TCP',
  name: 'HTTP(s) to MQTT Protocol Adapter',
  description:
    'Connects HiveMQ Edge to arbitrary web endpoint URLs via HTTP(s), consuming structured JSON or plain data.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#http-adapter',
  version: 'Development Version',
  logoUrl: '/module/images/http-icon.png',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['READ'],
  category: {
    name: 'CONNECTIVITY',
    displayName: 'Connectivity',
    description: 'A standard connectivity based protocol, typically web standard.',
  },
  tags: ['INTERNET', 'TCP', 'WEB'],
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      allowUntrustedCertificates: {
        type: 'boolean',
        title: 'Allow Untrusted Certificates',
        description: 'Allow the adapter to connect to untrusted SSL sources (for example expired certificates).',
        default: false,
        format: 'boolean',
      },
      httpConnectTimeoutSeconds: {
        type: 'integer',
        title: 'HTTP Connection Timeout',
        description: 'Timeout (in seconds) to allow the underlying HTTP connection to be established',
        default: 5,
        minimum: 1,
        maximum: 60,
      },
      httpToMqtt: {
        type: 'object',
        properties: {
          assertResponseIsJson: {
            type: 'boolean',
            title: 'Assert JSON Response?',
            description:
              'Always attempt to parse the body of the response as JSON data, regardless of the Content-Type on the response.',
            default: false,
            format: 'boolean',
          },
          httpPublishSuccessStatusCodeOnly: {
            type: 'boolean',
            title: 'Publish Only On Success Codes',
            description: 'Only publish data when HTTP response code is successful ( 200 - 299 )',
            default: true,
            format: 'boolean',
          },
          maxPollingErrorsBeforeRemoval: {
            type: 'integer',
            title: 'Max. Polling Errors',
            description: 'Max. errors polling the endpoint before the polling daemon is stopped',
            default: 10,
            minimum: 3,
          },
          pollingIntervalMillis: {
            type: 'integer',
            title: 'Polling Interval [ms]',
            description: 'Time in millisecond that this endpoint will be polled',
            default: 1000,
            minimum: 1,
          },
        },
        title: 'HTTP To MQTT Config',
        description: 'The configuration for a data stream from HTTP to MQTT',
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
    required: ['id'],
  },
  uiSchema: {
    'ui:tabs': [
      {
        id: 'coreFields',
        title: 'Connection',
        properties: ['id', 'httpConnectTimeoutSeconds', 'allowUntrustedCertificates'],
      },
      {
        id: 'httpToMqtt',
        title: 'HTTP to MQTT',
        properties: ['httpToMqtt'],
      },
      {
        id: 'mqttToHttp',
        title: 'MQTT to HTTP',
        properties: ['mqttToHttp'],
      },
    ],
    id: {
      'ui:disabled': true,
    },
    'ui:order': ['id', 'url', '*'],
    httpToMqtt: {
      'ui:batchMode': true,
      'ui:order': [
        'httpToMqttMappings',
        'pollingIntervalMillis',
        'maxPollingErrorsBeforeRemoval',
        'assertResponseIsJson',
        'httpPublishSuccessStatusCodeOnly',
        '*',
      ],
      httpToMqttMappings: {
        'ui:batchMode': true,
        items: {
          'ui:order': [
            'url',
            'mqttTopic',
            'mqttQos',
            'mqttUserProperties',
            'httpRequestMethod',
            'httpRequestTimeoutSeconds',
            'httpRequestBodyContentType',
            'httpRequestBody',
            'httpHeaders',
            'includeTimestamp',
            '*',
          ],
          'ui:collapsable': {
            titleKey: 'mqttTopic',
          },
          httpRequestBody: {
            'ui:widget': 'textarea',
          },
        },
      },
    },
    mqttToHttp: {
      'ui:batchMode': true,
      'ui:order': ['mqttToHttpMappings', '*'],
      mqttToHttpMappings: {
        'ui:batchMode': true,
        items: {
          'ui:order': [
            'url',
            'mqttTopicFilter',
            'mqttMaxQos',
            'mqttUserProperties',
            'httpRequestMethod',
            'httpRequestTimeoutSeconds',
            'httpHeaders',
            '*',
          ],
          'ui:collapsable': {
            titleKey: 'mqttTopicFilter',
          },
        },
      },
    },
  },
}

export const MOCK_ADAPTER_HTTP: Adapter = {
  id: 'test-http',
  config: {
    httpConnectTimeoutSeconds: 5,
    httpToMqtt: {
      pollingIntervalMillis: 5000,
      maxPollingErrorsBeforeRemoval: 10,
      assertResponseIsJson: false,
      httpPublishSuccessStatusCodeOnly: true,
    },
    allowUntrustedCertificates: false,
    id: 'test-http',
  },
  status: {
    connection: Status.connection.STATELESS,
    id: 'test-http',
    runtime: Status.runtime.STARTED,
    startedAt: '2025-04-05T20:40:59.871Z',
    type: 'adapter',
  },
  type: 'http',
}

export const MOCK_SCHEMA_HTTP: TagSchema = {
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
