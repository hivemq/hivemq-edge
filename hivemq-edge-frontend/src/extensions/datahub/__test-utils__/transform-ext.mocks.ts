const NODE1_ID = 'node_a94a5dc9-c092-4131-81d4-eae9ff0e0207'
const NODE2_ID = 'node_d2dc73c3-4bce-4126-b6c7-8494b01fed83'
const NODE3_ID = 'node_cf866b22-340a-41da-a669-4589a30969f9'
const NODE4_ID = 'node_704a4cdc-e4c3-46d8-822c-0922f0b2de98'
const NODE5_ID = 'node_8532d17d-7cec-43ad-8864-9fba85b94113'

const STROKE_DEFAULT = 'var(--chakra-colors-green-500)'

export const MOCK_TRANSFORM_EXT = {
  nodes: [
    {
      id: 'node_f9ed3037-78e7-42e2-8027-0be046dba6f1',
      type: 'OPERATION',
      position: {
        x: 345,
        y: 375,
      },
      data: {
        functionId: 'Mqtt.drop',
        metadata: {
          isTerminal: true,
        },
      },
      width: 203,
      height: 48,
      selected: false,
      dragging: false,
      positionAbsolute: {
        x: 345,
        y: 375,
      },
    },
    {
      id: 'node_3c6bd879-7058-4ae0-bfb1-6d6b126c6495',
      type: 'SCHEMA',
      position: {
        x: 465,
        y: -15,
      },
      data: {
        type: 'JSON',
        version: '1',
        schemaSource:
          '{\n  "type": "object",\n  "title": "schema-for-fan",\n  "properties": {\n    "celsius": {\n      "type": "number"\n    },\n    "timestamp": {\n      "type": "number"\n    }\n  },\n  "required": [\n    "celsius",\n    "timestamp"\n  ]\n}',
      },
      width: 246,
      height: 74,
      selected: false,
      dragging: false,
      positionAbsolute: {
        x: 465,
        y: -15,
      },
    },
    {
      id: 'node_72457a3e-425c-48b1-aecb-71171a43edd7',
      type: 'TOPIC_FILTER',
      position: {
        x: -300,
        y: 300,
      },
      data: {
        topics: ['factory/#'],
      },
      width: 226,
      height: 48,
      selected: false,
      dragging: false,
      positionAbsolute: {
        x: -300,
        y: 300,
      },
    },
    {
      id: NODE1_ID,
      type: 'SCHEMA',
      position: {
        x: 60,
        y: -15,
      },
      data: {
        type: 'JSON',
        version: '1',
        schemaSource:
          '{\n  "type": "object",\n  "title": "schema-from-device",\n  "properties": {\n    "fahrenheit": {\n      "type": "number"\n    },\n    "timestamp": {\n      "type": "number"\n    }\n  },\n  "required": [\n    "fahrenheit",\n    "timestamp"\n  ]\n}',
      },
      width: 279,
      height: 74,
      selected: false,
      positionAbsolute: {
        x: 60,
        y: -15,
      },
      dragging: false,
    },
    {
      id: NODE2_ID,
      type: 'VALIDATOR',
      position: {
        x: 0,
        y: 150,
      },
      data: {
        type: 'schema',
        strategy: 'ALL_OF',
        schemas: [
          {
            version: '1',
            schemaId: 'first mock schema',
          },
        ],
      },
      width: 231,
      height: 80,
      selected: false,
      positionAbsolute: {
        x: 0,
        y: 150,
      },
      dragging: false,
    },
    {
      id: NODE5_ID,
      type: 'DATA_POLICY',
      position: {
        x: 0,
        y: 300,
      },
      data: {
        label: 'DATA_POLICY node',
      },
      width: 226,
      height: 68,
      selected: false,
      positionAbsolute: {
        x: 0,
        y: 300,
      },
      dragging: false,
    },
    {
      id: NODE4_ID,
      type: 'FUNCTION',
      position: {
        x: 315,
        y: 150,
      },
      data: {
        type: 'Javascript',
        name: 'fahrenheit-to-celsius',
        version: 'v1',
        sourceCode:
          '/**\n *\n * @param {Object} publish\n * @param {string} publish.topic    The MQTT topic that is currently specified for this PUBLISH packet.\n * @param {Object} publish.payload  A list of the name and value of all user properties of the MQTT 5 PUBLISH packet. This setting has no effect on MQTT 3 clients.\n * @param {Record<string, string>[]} publish.userProperties The JSON object representation of the deserialized MQTT payload.\n * @param {Object} context\n * @param {Record<string, string>[]} context.arguments  The arguments provided to the script. Currently, arguments can only be provided via a data policy.\n * @param {string} context.policyId The policy id of the policy from which the transformation function is called.\n * @param {string} context.clientId The client Id of the client from which the MQTT publish was sent.\n * @returns {Object} The publish-object is passed as a parameter into the transform function. The same object or a new object is returned as the transformed object.\n */\nfunction transform(publish, context) {\n    publish.payload = {\n        "celsius": convert(publish.payload.fahrenheit),\n        "timestamp": publish.payload.timestamp\n    }\n\n    return publish;\n}\n\n/// TODO[NVL] Can you spot the typo?\nfunction convert(fahrenheit) {\n    return Mah.floor((fahrenheit - 32) * 5/9);\n}\n',
      },
      width: 331,
      height: 74,
      selected: false,
      dragging: false,
      positionAbsolute: {
        x: 315,
        y: 150,
      },
    },
    {
      id: NODE3_ID,
      type: 'OPERATION',
      position: {
        x: 345,
        y: 285,
      },
      data: {
        functionId: 'DataHub.transform',
        formData: {},
        metadata: {
          isTerminal: false,
          hasArguments: true,
        },
      },
      width: 270,
      height: 48,
      selected: false,
      dragging: false,
      positionAbsolute: {
        x: 345,
        y: 285,
      },
    },
  ],
  edges: [
    {
      source: NODE2_ID,
      sourceHandle: 'source',
      target: NODE5_ID,
      targetHandle: 'validation',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_d2dc73c3-4bce-4126-b6c7-8494b01fed83source-node_8532d17d-7cec-43ad-8864-9fba85b94113validation',
      selected: false,
    },
    {
      source: NODE1_ID,
      sourceHandle: 'source',
      target: NODE2_ID,
      targetHandle: 'target',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_a94a5dc9-c092-4131-81d4-eae9ff0e0207source-node_d2dc73c3-4bce-4126-b6c7-8494b01fed83target',
    },
    {
      source: NODE4_ID,
      sourceHandle: 'source',
      target: NODE3_ID,
      targetHandle: 'schema',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_704a4cdc-e4c3-46d8-822c-0922f0b2de98source-node_cf866b22-340a-41da-a669-4589a30969f9schema',
    },
    {
      source: NODE5_ID,
      sourceHandle: 'onError',
      target: 'node_f9ed3037-78e7-42e2-8027-0be046dba6f1',
      targetHandle: 'input',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_8532d17d-7cec-43ad-8864-9fba85b94113onError-node_f9ed3037-78e7-42e2-8027-0be046dba6f1input',
    },
    {
      source: 'node_72457a3e-425c-48b1-aecb-71171a43edd7',
      sourceHandle: 'factory/#-0',
      target: NODE5_ID,
      targetHandle: 'topicFilter',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_72457a3e-425c-48b1-aecb-71171a43edd7factory/#-0-node_8532d17d-7cec-43ad-8864-9fba85b94113topicFilter',
    },
    {
      source: 'node_3c6bd879-7058-4ae0-bfb1-6d6b126c6495',
      sourceHandle: 'source',
      target: NODE4_ID,
      targetHandle: 'deserialiser',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_3c6bd879-7058-4ae0-bfb1-6d6b126c6495source-node_704a4cdc-e4c3-46d8-822c-0922f0b2de98deserialiser',
    },
    {
      source: NODE1_ID,
      sourceHandle: 'source',
      target: NODE4_ID,
      targetHandle: 'serialiser',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_a94a5dc9-c092-4131-81d4-eae9ff0e0207source-node_704a4cdc-e4c3-46d8-822c-0922f0b2de98serialiser',
    },
    {
      source: NODE5_ID,
      sourceHandle: 'onSuccess',
      target: NODE3_ID,
      targetHandle: 'input',
      markerEnd: {
        type: 'arrowclosed',
        width: 20,
        height: 20,
        color: '#008c2d',
      },
      style: {
        strokeWidth: 2,
        stroke: STROKE_DEFAULT,
      },
      id: 'reactflow__edge-node_8532d17d-7cec-43ad-8864-9fba85b94113onSuccess-node_cf866b22-340a-41da-a669-4589a30969f9input',
    },
  ],
}
