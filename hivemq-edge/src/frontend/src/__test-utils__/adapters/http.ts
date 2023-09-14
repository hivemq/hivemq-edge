import { ProtocolAdapter } from '@/api/__generated__'

export const MOCK_PROTOCOL_HTTP: ProtocolAdapter = {
  id: 'http',
  protocol: 'HTTP(s) over TCP',
  name: 'HTTP(s) to MQTT Protocol Adapter',
  description:
    'Connects HiveMQ Edge to arbitrary web endpoint URLs via HTTP(s), consuming structured JSON or plain data.',
  url: 'https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#http',
  version: 'Development Snapshot (BETA)',
  logoUrl: 'http://localhost:8080/images/http-icon.png',
  author: 'HiveMQ',
  installed: true,
  // capabilities: ['READ'],
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
      url: {
        type: 'string',
        title: 'URL',
        description: 'The url of the http request you would like to make',
        pattern:
          'https?:\\/\\/(?:w{1,3}\\.)?[^\\s.]+(?:\\.[a-z]+)*(?::\\d+)?((?:\\/\\w+)|(?:-\\w+))*\\/?(?![^<]*(?:<\\/\\w+>|\\/?>))',
      },
      destination: {
        type: 'string',
        title: 'Destination Topic',
        description: 'The topic to publish data on',
        format: 'mqtt-topic',
      },
      qos: {
        type: 'integer',
        title: 'QoS',
        description: 'MQTT Quality of Service level',
        default: 0,
        minimum: 0,
        maximum: 2,
      },
      httpRequestMethod: {
        type: 'string',
        enum: ['GET', 'POST', 'PUT'],
        title: 'Http Method',
        description: 'Http method associated with the request',
        default: 'GET',
      },
      httpConnectTimeout: {
        type: 'integer',
        title: 'Http Connection Timeout',
        description: 'Timeout (in second) to apply to the HTTP Request',
        default: 10,
      },
      httpRequestBodyContentType: {
        type: 'string',
        enum: ['JSON', 'PLAIN', 'HTML', 'XML', 'YAML'],
        title: 'Http Request Content Type',
        description: 'Content Type associated with the request',
        default: 'JSON',
      },
      httpRequestBody: {
        type: 'string',
        title: 'Http Request Body',
        description: 'The body to include in the HTTP request',
      },
      httpPublishSuccessStatusCodeOnly: {
        type: 'boolean',
        title: 'Only publish data when HTTP response code is successful ( 200 - 299 )',
        default: true,
        format: 'boolean',
      },
      httpHeaders: {
        title: 'HTTP Headers',
        description: 'HTTP headers to be added to your requests',
        type: 'array',
        items: {
          type: 'object',
          properties: {
            name: {
              type: 'string',
              title: 'Http Header Name',
              description: 'The name of the HTTP header',
            },
            value: {
              type: 'string',
              title: 'Http Header Value',
              description: 'The value of the HTTP header',
            },
          },
          title: 'HTTP Headers',
          description: 'HTTP headers to be added to your requests',
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
      publishingInterval: {
        type: 'integer',
        title: 'Polling interval [ms]',
        description: 'Time in millisecond that this URL will be called',
        default: 1000,
        minimum: 1,
      },
      maxPollingErrorsBeforeRemoval: {
        type: 'integer',
        title: 'Max. Polling Errors',
        description: 'Max. errors polling the endpoint before the polling daemon is stopped',
        default: 10,
        minimum: 3,
      },
    },
    required: ['url', 'destination', 'qos', 'httpConnectTimeout', 'id', 'publishingInterval'],
  },
}

export const MOCK_ADAPTER_HTTP = {
  id: '678',
  type: 'http',
  config: {
    url: 'http://s',
    destination: 'a/valid/topic/http/1',
    qos: 0,
    httpRequestMethod: 'GET',
    httpConnectTimeout: 10,
    httpRequestBodyContentType: 'JSON',
    httpPublishSuccessStatusCodeOnly: true,
    httpHeaders: [],
    id: '678',
    publishingInterval: 1000,
    maxPollingErrorsBeforeRemoval: 10,
  },
  adapterRuntimeInformation: {
    lastStartedAttemptTime: '2023-09-13T09:52:07.554+01',
    numberOfDaemonProcesses: 1,
    connectionStatus: {
      status: 'CONNECTED',
      id: '678',
      type: 'adapter',
    },
  },
}
