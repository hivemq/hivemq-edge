import { RJSFSchema } from '@rjsf/utils'

export const MqttUserProperty: RJSFSchema = {
  title: 'MQTT User Properties',
  description: 'Arbitrary properties to associate with the mapping',
  maxItems: 10,
  type: 'array',
  items: {
    type: 'object',
    properties: {
      name: {
        type: 'string',
        title: 'Name',
        description: 'Name of the associated property',
      },
      value: {
        type: 'string',
        title: 'Value',
        description: 'Value of the associated property',
      },
    },
    required: ['name', 'value'],
    maxItems: 10,
  },
}
