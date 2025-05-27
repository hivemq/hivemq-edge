import { http, HttpResponse } from 'msw'
import type { FunctionSpecs, FunctionSpecsList, JsonNode } from '@/api/__generated__'
import { BehaviorPolicyTransitionEvent } from '@/api/__generated__'

import mockFunctions from '@datahub/api/__generated__/schemas/_functions.json'

export const MOCK_DATAHUB_FUNCTIONS_MQTT_USER_PROPERTY: FunctionSpecs = {
  functionId: 'Mqtt.UserProperties.add',
  metadata: {
    inLicenseAllowed: false,
    isTerminal: false,
    isDataOnly: false,
    hasArguments: true,
    supportedEvents: [
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
    ],
  },
  schema: {
    title: 'Mqtt.UserProperties.add',
    description: 'Adds a user property to the MQTT message.',
    type: 'object',
    required: ['name', 'value'],
    properties: {
      name: {
        type: 'string',
        title: 'name',
        description:
          'Specifies the name of the user property. Multiple user properties with the same name are allowed.',
        format: 'interpolation',
      },
      value: {
        type: 'string',
        title: 'value',
        description: 'Specifies the value of the user property.',
        format: 'interpolation',
      },
    },
  },
}
export const MOCK_DATAHUB_FUNCTIONS_DELIVERY_REDIRECT: FunctionSpecs = {
  functionId: 'Delivery.redirectTo',
  metadata: {
    inLicenseAllowed: true,
    isTerminal: true,
    isDataOnly: true,
    hasArguments: true,
    supportedEvents: [],
  },
  schema: {
    title: 'Delivery.redirectTo',
    description: 'Redirects an MQTT PUBLISH message to a specified topic.',
    type: 'object',
    required: ['topic', 'applyPolicies'],
    properties: {
      topic: {
        type: 'string',
        title: 'topic',
        description: 'The destination MQTT topic according to the MQTT specification.',
        format: 'interpolation',
      },
      applyPolicies: {
        type: 'boolean',
        title: 'applyPolicies',
        description: 'Defines whether policies are executed after publishing to a different topic.',
        format: 'interpolation',
      },
    },
  },
}
export const MOCK_DATAHUB_FUNCTIONS_SYSTEM_LOG: FunctionSpecs = {
  functionId: 'System.log',
  metadata: {
    inLicenseAllowed: false,
    isTerminal: false,
    isDataOnly: false,
    hasArguments: true,
    supportedEvents: [
      BehaviorPolicyTransitionEvent.EVENT_ON_ANY,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
      BehaviorPolicyTransitionEvent.CONNECTION_ON_DISCONNECT,
    ],
  },
  schema: {
    title: 'System.log',
    description: 'Logs a message on the given level.',
    type: 'object',
    required: ['level', 'message'],
    properties: {
      level: {
        type: 'string',
        title: 'Log Level',
        description: 'Specifies the log level of the function.',
        format: 'interpolation',
      },
      message: {
        type: 'string',
        title: 'Message',
        description:
          'Adds a user-defined string that prints to the log file. For more information, see Example log message.',
        format: 'interpolation',
      },
    },
  },
}
export const MOCK_DATAHUB_FUNCTIONS_SERDES_SERIALIZE: FunctionSpecs = {
  functionId: 'Serdes.serialize',
  metadata: {
    inLicenseAllowed: true,
    isTerminal: false,
    isDataOnly: true,
    hasArguments: true,
    supportedEvents: [],
  },
  schema: {
    title: 'Serdes.serialize',
    description:
      'Serializes a data object into a binary MQTT message payload based on the configured JSON or Protobuf Schema.',
    type: 'object',
    required: ['schemaId', 'schemaVersion'],
    properties: {
      schemaId: {
        type: 'string',
        title: 'schemaId',
        description: 'The identifier of the JSON or Protobuf Schema to be used for serialization',
        format: 'interpolation',
      },
      schemaVersion: {
        type: 'string',
        title: 'schemaVersion',
        description: 'The version of the schema to be used for serialization.',
        format: 'interpolation',
      },
    },
  },
}
export const MOCK_DATAHUB_FUNCTIONS_SERDES_DESERIALIZE: FunctionSpecs = {
  functionId: 'Serdes.deserialize',
  metadata: {
    inLicenseAllowed: true,
    isTerminal: false,
    isDataOnly: true,
    hasArguments: true,
    supportedEvents: [],
  },
  schema: {
    title: 'Serdes.deserialize',
    description:
      'Deserializes a binary MQTT message payload into a data object based on the configured JSON or Protobuf Schema.',
    type: 'object',
    required: ['schemaId', 'schemaVersion'],
    properties: {
      schemaId: {
        type: 'string',
        title: 'schemaId',
        description: 'The identifier of the JSON or Protobuf Schema to be used for deserialization.',
        format: 'interpolation',
      },
      schemaVersion: {
        type: 'string',
        title: 'schemaVersion',
        description: 'The version of the schema to be used for deserialization.',
        format: 'interpolation',
      },
    },
  },
}
export const MOCK_DATAHUB_FUNCTIONS_METRICS_COUNTER_INC: FunctionSpecs = {
  functionId: 'Metrics.Counter.increment',
  metadata: {
    inLicenseAllowed: false,
    isTerminal: false,
    isDataOnly: false,
    hasArguments: true,
    supportedEvents: [
      BehaviorPolicyTransitionEvent.EVENT_ON_ANY,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
      BehaviorPolicyTransitionEvent.CONNECTION_ON_DISCONNECT,
    ],
  },
  schema: {
    title: 'Metrics.Counter.increment',
    description: 'Increments a metric of type counter, which can be accessed with monitoring.',
    type: 'object',
    required: ['metricName', 'incrementBy'],
    properties: {
      metricName: {
        type: 'string',
        title: 'metricName',
        description: 'Specifies the name of the metric to be incremented.',
      },
      incrementBy: {
        type: 'integer',
        title: 'incrementBy',
        description: 'Specifies the amount by which the counter should be incremented. Negative values are supported.',
        format: 'interpolation',
      },
    },
  },
}
export const MOCK_DATAHUB_FUNCTIONS_MQTT_DISCONNECT: FunctionSpecs = {
  functionId: 'Mqtt.disconnect',
  metadata: {
    inLicenseAllowed: false,
    isTerminal: true,
    isDataOnly: false,
    hasArguments: false,
    supportedEvents: [
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
    ],
  },
  schema: {
    title: 'Mqtt.disconnect',
    description: 'Disconnects the client.',
    type: 'object',
    required: [],
    properties: {},
  },
}
export const MOCK_DATAHUB_FUNCTIONS_MQTT_DROP: FunctionSpecs = {
  functionId: 'Mqtt.drop',
  metadata: {
    inLicenseAllowed: false,
    isTerminal: true,
    isDataOnly: false,
    hasArguments: true,
    supportedEvents: [
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
    ],
  },
  schema: {
    title: 'Mqtt.drop',
    description: 'Drops the MQTT packet that is currently processed.',
    type: 'object',
    required: [],
    properties: {
      reasonString: {
        type: 'string',
        title: 'reasonString',
        description: 'Specifies the reason string that is returned to MQTT 5 clients.',
        format: 'interpolation',
      },
    },
  },
}

export const MOCK_DATAHUB_FUNCTIONS: FunctionSpecsList = {
  items: [
    MOCK_DATAHUB_FUNCTIONS_MQTT_USER_PROPERTY,
    MOCK_DATAHUB_FUNCTIONS_DELIVERY_REDIRECT,
    MOCK_DATAHUB_FUNCTIONS_SYSTEM_LOG,
    MOCK_DATAHUB_FUNCTIONS_SERDES_SERIALIZE,
    MOCK_DATAHUB_FUNCTIONS_SERDES_DESERIALIZE,
    MOCK_DATAHUB_FUNCTIONS_METRICS_COUNTER_INC,
    MOCK_DATAHUB_FUNCTIONS_MQTT_DISCONNECT,
    MOCK_DATAHUB_FUNCTIONS_MQTT_DROP,
  ],
}

export const handlers = [
  http.get('*/data-hub/functions', () => {
    return HttpResponse.json<JsonNode>(mockFunctions, { status: 200 })
  }),

  http.get('*/data-hub/function-specs', () => {
    return HttpResponse.json(MOCK_DATAHUB_FUNCTIONS, { status: 200 })
  }),
]
