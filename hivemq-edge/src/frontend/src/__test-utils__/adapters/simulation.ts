import { GenericObjectType } from '@rjsf/utils'
import { ProtocolAdapter } from '@/api/__generated__'

export const MOCK_PROTOCOL_SIMULATION: ProtocolAdapter = {
  id: 'simulation',
  protocol: 'Simulation',
  name: 'Simulated Edge Device',
  description: 'Without needing to configure real devices, simulate traffic from an edge device into HiveMQ Edge.',
  url: 'https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#simulation',
  version: 'Development Snapshot',
  logoUrl: 'http://localhost:8080/images/hivemq-icon.png',
  author: 'HiveMQ',
  installed: true,
  // capabilities: ['READ'],
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
      id: {
        type: 'string',
        title: 'Identifier',
        description: 'Unique identifier for this protocol adapter',
        minLength: 1,
        maxLength: 1024,
        format: 'identifier',
        pattern: '([a-zA-Z_0-9\\-])*',
      },
      pollingIntervalMillis: {
        type: 'integer',
        title: 'Polling interval [ms]',
        description: 'Interval in milliseconds to poll for changes',
        default: 10000,
        minimum: 100,
        maximum: 86400000,
      },
      subscriptions: {
        title: 'Subscriptions',
        description: 'List of subscriptions for the simulation',
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
          description: 'List of subscriptions for the simulation',
        },
      },
    },
    required: ['id', 'subscriptions'],
  },
}

export const MOCK_ADAPTER_SIMULATION: GenericObjectType = {
  id: '345',
  type: 'simulation',
  config: {
    id: '345',
    pollingIntervalMillis: 10000,
    subscriptions: [
      {
        destination: 'a/valid/topic/simulation/1',
        qos: 0,
      },
    ],
  },
  adapterRuntimeInformation: {
    lastStartedAttemptTime: '2023-09-13T09:45:02.202+01',
    numberOfDaemonProcesses: 1,
    connectionStatus: {
      status: 'CONNECTED',
      id: '345',
      type: 'adapter',
    },
  },
}
