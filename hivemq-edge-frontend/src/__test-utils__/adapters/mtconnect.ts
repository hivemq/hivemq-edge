import type { ProtocolAdapter } from '@/api/__generated__'

export const MOCK_PROTOCOL_MTCONNECT: ProtocolAdapter = {
  id: 'mtconnect',
  protocol: 'MTConnect',
  name: 'MTConnect Protocol Adapter',
  description: 'Connects HiveMQ Edge to existing MTConnect devices.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#mtconnect-protocol-adapter',
  version: 'Development Version',
  logoUrl: '/module/images/mtconnect-icon.png',
  author: 'HiveMQ',
  installed: true,
  capabilities: ['READ'],
  category: {
    name: 'INDUSTRIAL',
    displayName: 'Industrial',
    description: 'Industrial, typically field bus protocols.',
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
    required: ['id', 'pollingIntervalMillis'],
  },
  uiSchema: {
    'ui:tabs': [
      {
        id: 'coreFields',
        title: 'Connection',
        properties: ['id'],
      },
    ],
    'ui:order': ['id', '*'],
  },
}
