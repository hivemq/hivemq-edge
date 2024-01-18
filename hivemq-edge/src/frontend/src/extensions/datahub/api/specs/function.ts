/*****************************************************
 * Non-terminal functions
 *****************************************************/
import { FunctionSpecs } from '@/extensions/datahub/types.ts'

export const systemLog: FunctionSpecs = {
  functionId: 'System.Log',
  isTerminal: false,
  schema: {
    title: 'System.Log',
    description: 'Logs a message on the given level',
    required: ['level', 'message'],
    type: 'object',
    properties: {
      level: {
        type: 'string',
        title: 'Log Level',
        description: 'Specifies the log level of the function in the hivemq.log file',
        enum: ['DEBUG', 'ERROR', 'WARN', 'INFO', 'TRACE'],
      },
      message: {
        type: 'string',
        title: 'Message',
        description:
          'Adds a user-defined string that prints to the log file. For more information, see Example log message',
      },
    },
  },
  uiSchema: {
    message: {
      'ui:widget': 'textarea',
      'ui:placeholder': 'Leaving this field empty will cause formData property to be `null`',
      'ui:emptyValue': null,
    },
  },
}

export const metricsCounterIncrement: FunctionSpecs = {
  functionId: 'Metrics.Counter.increment',
  isTerminal: false,
  schema: {
    title: 'Metrics.Counter.increment',
    description: 'Increments a metric of type counter, which can be accessed with monitoring',
    required: ['metricName', 'incrementBy'],
    type: 'object',
    properties: {
      metricName: {
        type: 'string',
        title: 'Metric Name',
        description: 'Specifies the name of the metric to be incremented',
      },
      incrementBy: {
        type: 'number',
        title: 'IncrementBy',
        description: 'Specifies the amount by which the counter should be incremented. Negative values are supported',
      },
    },
  },
  uiSchema: {
    incrementBy: {
      'ui:widget': 'updown',
    },
    metricName: {
      // 'ui:widget': (props: WidgetProps) => {
      //   return (
      //     <InputGroup>
      //       <InputLeftAddon>com.hivemq.data-hub.custom.counters.</InputLeftAddon>
      //       <Input
      //         isRequired={props.required}
      //         placeholder="metricName"
      //         value={props.value}
      //         onChange={(event) => props.onChange(event.target.value)}
      //       />
      //     </InputGroup>
      //   )
      // },
    },
  },
}

export const mqttUserPropertiesAdd: FunctionSpecs = {
  functionId: 'Mqtt.UserProperties.add',
  isTerminal: false,
  schema: {
    title: 'Mqtt.UserProperties.add',
    description: 'Adds a user property to the MQTT message',
    required: ['name', 'value'],
    type: 'object',
    properties: {
      name: {
        type: 'string',
        title: 'Property Name',
        description:
          'Specifies the name of the user property. Multiple user properties with the same name are allowed.',
      },
      value: {
        type: 'string',
        title: 'Property Value',
        description: 'Specifies the value of the user property.',
      },
    },
  },
}

export const serdesDeserialize: FunctionSpecs = {
  functionId: 'Serdes.deserialize',
  isTerminal: false,
  isDataOnly: true,
  hasArguments: true,
  schema: {
    title: 'Serdes.deserialize',
    description:
      'Deserializes a binary MQTT message payload into a data object based on the configured JSON Schema or Protobuf schema.',
    required: ['schemaId', 'schemaVersion'],
    type: 'object',
    properties: {
      schemaId: {
        type: 'string',
        title: 'Schema ID',
        description: 'The identifier of the JSON Schema or Protobuf schema to be used for deserialization.',
      },
      schemaVersion: {
        type: 'string',
        title: 'Schema Version',
        description: 'The version of the schema to be used for deserialization.',
      },
    },
  },
}

export const serdesSerialize: FunctionSpecs = {
  functionId: 'Serdes.serialize',
  isTerminal: false,
  isDataOnly: true,
  hasArguments: true,
  schema: {
    title: 'Serdes.serialize',
    description:
      'Serializes a data object into a binary MQTT message payload based on the configured JSON Schema or Protobuf schema.',
    required: ['schemaId', 'schemaVersion'],
    type: 'object',
    properties: {
      schemaId: {
        type: 'string',
        title: 'Schema ID',
        description: 'The identifier of the JSON Shcema to be used for serialization',
      },
      schemaVersion: {
        type: 'string',
        title: 'Schema Version',
        description: 'The version of the schema to be used for serialization.',
      },
    },
  },
}

/*****************************************************
 * terminal functions
 *****************************************************/

export const deliveryRedirectTo: FunctionSpecs = {
  functionId: 'Delivery.redirectTo',
  isTerminal: true,
  isDataOnly: true,
  schema: {
    title: 'Delivery.redirectTo',
    description: 'Redirects an MQTT PUBLISH message to a specified topic',
    required: ['topic'],
    type: 'object',
    properties: {
      topic: {
        type: 'string',
        title: 'Topic',
        description: 'The destination MQTT topic according to MQTT specification',
      },
      applyPolicies: {
        type: 'boolean',
        title: 'Apply Policies',
        description: 'Defines whether policies are executed after publishing to a different topic.',
      },
    },
  },
}

export const mqttDisconnect: FunctionSpecs = {
  functionId: 'Mqtt.disconnect',
  isTerminal: true,
  schema: {
    title: 'Mqtt.disconnect',
    description: 'Redirects an MQTT PUBLISH message to a specified topic',
    properties: {},
  },
}

export const mqttDrop: FunctionSpecs = {
  functionId: 'Mqtt.drop',
  isTerminal: true,
  schema: {
    title: 'Mqtt.drop',
    description: 'Drops the MQTT packet that is currently processed',
    properties: {},
  },
}

/*****************************************************
 * Custom functions
 *****************************************************/

export const customJSFunction: FunctionSpecs = {
  functionId: 'custom.JS.function',
  isTerminal: false,
  schema: {
    title: 'custom.JS.function',
    description: 'Raw JS Custom function',
    properties: {
      transform: {
        type: 'string',
        format: 'editor:js',
      },
    },
  },
  uiSchema: {
    transform: {
      'ui:widget': 'editorJS',
    },
  },
}

export const datahubInternalFunctions = [
  systemLog,
  deliveryRedirectTo,
  mqttUserPropertiesAdd,
  serdesDeserialize,
  serdesSerialize,
  metricsCounterIncrement,
  mqttDisconnect,
  mqttDrop,
]
