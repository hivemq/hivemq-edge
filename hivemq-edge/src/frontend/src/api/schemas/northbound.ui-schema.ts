import { UiSchema } from '@rjsf/utils'

/* istanbul ignore next -- @preserve */
export const northboundMappingListUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },

  items: {
    'ui:title': 'List of northbound mappings',
    'ui:description': 'The list of all the mappings delivering messages from this adapter onto the Edge',
    items: {
      'ui:order': ['tagName', 'topic', '*'],
      'ui:collapsable': {
        titleKey: 'tagName',
      },
      'ui:addButton': 'Add a mapping',
      userProperties: {
        items: {
          'ui:addButton': 'Add a user property',
        },
      },
      maxQoS: {
        'ui:enumNames': ['At most once (QoS 0)', 'At least once (QoS 1)', 'Exactly once (QoS 2)'],
      },

      messageHandlingOptions: {
        'ui:enumNames': [
          'MQTT Message Per Device Tag',
          'MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)',
        ],
      },
    },
  },
}
