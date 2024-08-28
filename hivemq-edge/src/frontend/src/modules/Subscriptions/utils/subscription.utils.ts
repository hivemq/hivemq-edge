import { type RJSFSchema, UiSchema } from '@rjsf/utils'

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
    subscriptions: {
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
