import { type RJSFSchema, UiSchema } from '@rjsf/utils'
import { OutwardMapping } from '@/modules/Mappings/types.ts'

interface MockSubscription {
  schema?: RJSFSchema
  uiSchema?: UiSchema
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export const MOCK_OUTWARD_SUBSCRIPTION_OPCUA: MockSubscription = {
  schema: {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    type: 'object',
    properties: {
      subscriptions: {
        type: 'array',
        items: {
          type: 'object',
          required: ['node', 'mqtt-topic'],
          properties: {
            node: {
              type: 'string',
              title: 'Destination Node ID',
              description: 'identifier of the node on the OPC-UA server. Example: "ns=3;s=85/0:Temperature"',
            },
            'mqtt-topic': {
              type: 'array',
              title: 'Source Topics',
              description: 'The MQTT topics used to identify the source of the mapping',
              items: {
                type: 'string',
                uniqueItems: true,
                format: 'mqtt-topic',
              },
            },
            mapping: {
              type: 'array',
              title: 'Mapping instructions',
              description:
                'The list of data model transformations required to produce a valid destination node from the selected MQTT topics',
              maxItems: 20,
              items: {
                type: 'object',
                properties: {
                  source: {
                    type: 'array',
                    description: 'The path of the property to use from the source data model',
                    items: {
                      type: 'string',
                    },
                  },
                  destination: {
                    type: 'string',
                    description: 'The path of the property to populate in the destination data model',
                  },
                  transformation: {
                    type: 'object',
                    description: 'The transformation to apply to the source data points',
                    properties: {
                      function: {
                        enum: ['toString', 'toInt', 'join'],
                      },
                      params: {
                        type: 'string',
                      },
                    },
                  },
                },
              },
            },
          },
        },
      },
    },
    required: ['subscriptions'],
  },
  uiSchema: {
    'ui:submitButtonOptions': {
      norender: true,
    },
    subscriptions: {
      'ui:field': 'mqtt:transform',
      items: {
        'mqtt-topic': {
          items: {
            'ui:options': { create: false, multiple: false },
          },
        },
      },
    },
  },
}

export const MOCK_MAPPING_DATA: OutwardMapping[] = [
  {
    'mqtt-topic': ['bar/test8', 'pump1/temperature'],
    mapping: [
      {
        source: ['dfdf'],
        destination: 'dfdf',
        transformation: {
          function: 'toString',
          params: 'dffd',
        },
      },
      {
        source: ['dd'],
        destination: 'dffdfd',
        transformation: {
          function: 'toString',
          params: 'fdfgfg',
        },
      },
    ],
    node: 'write/power-management/alert',
  },
  {
    'mqtt-topic': [],
    mapping: [],
    node: '',
  },
  {
    'mqtt-topic': [],
    mapping: [],
    node: '',
  },
]
