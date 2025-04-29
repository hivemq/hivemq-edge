import type { Adapter, ProtocolAdapter, TagSchema } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const MOCK_PROTOCOL_SIMULATION: ProtocolAdapter = {
  id: 'simulation',
  protocol: 'Simulation',
  name: 'Simulated Edge Device',
  description:
    'Simulates device message traffic to enable observation of adapter behavior without the need to configure actual devices.',
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
  tags: [],
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      minValue: {
        type: 'integer',
        title: 'Min. Generated Value',
        description: 'Minimum value of the generated decimal number',
        default: 0,
        minimum: 0,
      },
      maxValue: {
        type: 'integer',
        title: 'Max. Generated Value (Excl.)',
        description: 'Maximum value of the generated decimal number (excluded)',
        default: 1000,
        maximum: 1000,
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
        },
        title: 'simulationToMqtt',
        description: 'Define Simulations to create MQTT messages.',
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
    required: ['id', 'simulationToMqtt'],
  },
  uiSchema: {
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
      'ui:batchMode': true,
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
  },
}

export const MOCK_ADAPTER_SIMULATION: Adapter = {
  id: 'sim-current',
  config: {
    minValue: 0,
    maxValue: 1000,
    simulationToMqtt: {
      pollingIntervalMillis: 1000,
      maxPollingErrorsBeforeRemoval: 10,
    },
    minDelay: 0,
    maxDelay: 0,
    id: 'sim-current',
  },
  status: {
    connection: Status.connection.STATELESS,
    id: 'sim-current',
    runtime: Status.runtime.STARTED,
    startedAt: '2025-04-01T10:18:34.267Z',
    type: 'adapter',
  },
  type: 'simulation',
}

export const MOCK_SCHEMA_SIMULATION: TagSchema = {
  configSchema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      definition: {
        type: 'object',
        title: 'definition',
        description: "The simulation adapter doesn't currently support any custom definition",
        readOnly: true,
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
  protocolId: 'simulation',
}
