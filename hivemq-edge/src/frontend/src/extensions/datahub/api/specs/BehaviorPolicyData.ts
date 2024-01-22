import { BehaviorPolicyType, PanelSpecs } from '@/extensions/datahub/types.ts'

export const MOCK_VBEHAVIOR_POLICY_SCHEMA: PanelSpecs = {
  schema: {
    type: 'object',
    required: ['type'],
    properties: {
      type: {
        title: 'Behavior Model',
        enum: [BehaviorPolicyType.MQTT_EVENT, BehaviorPolicyType.PUBLISH_DUPLICATE, BehaviorPolicyType.PUBLISH_QUOTA],
        default: BehaviorPolicyType.MQTT_EVENT,
      },
    },
    // TODO[NVL] Not sure that's thw best approach, check for schema dependencies
    allOf: [
      {
        if: {
          properties: {
            type: {
              const: BehaviorPolicyType.PUBLISH_QUOTA,
            },
          },
        },
        then: {
          properties: {
            arguments: {
              type: 'object',
              title: 'Publish.quota options',
              description:
                'When you configure a publish-quota model, at least one of the available arguments must be present. Data Hub uses the default value for the missing parameter.\n' +
                'The default value for minimum is 0. The default value for maxPublishes is UNLIMITED.',
              required: ['minPublishes'],
              properties: {
                minPublishes: {
                  title: 'Minimum number of messages',
                  description: 'Defines the minimal number of published messages that must be reached',
                  type: 'number',
                  default: 0,
                  minimum: 0,
                },
                maxPublishes: {
                  title: 'Maximum number of messages',
                  description: 'Defines the maximum number of published messages that must be reached',
                  type: 'number',
                  default: 10000,
                  minimum: 0,
                  maximum: Infinity,
                },
              },
            },
          },
        },
      },
    ],
  },
}
