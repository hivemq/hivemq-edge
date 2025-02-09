import type { UiSchema } from '@rjsf/utils'

/* istanbul ignore next -- @preserve */
const metadataWidget: UiSchema = {
  // 'ui:widget': 'data-url',
  'ui:options': {
    accept: '.json',
  },
}

/* istanbul ignore next -- @preserve */
export const southboundMappingListUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },

  items: {
    'ui:title': 'List of Southbound mappings',
    'ui:description': 'The list of all the mappings delivering messages from Edge to the device',
    'ui:field': 'mqtt:transform',
    items: {
      'ui:order': ['topicFilter', 'tagName', '*'],
      'ui:collapsable': {
        titleKey: 'topicFilter',
      },
      'ui:addButton': 'Create a new mapping',

      topicFilter: {
        'ui:format': 'mqtt-topic=xx',
      },

      maxQoS: {
        'ui:enumNames': ['At most once (QoS 0)', 'At least once (QoS 1)', 'Exactly once (QoS 2)'],
      },

      fieldMapping: {
        metadata: {
          // 'ui:widget': 'hidden',
          destination: {
            ...metadataWidget,
          },
          source: {
            ...metadataWidget,
          },
        },
        instructions: {
          items: {
            'ui:addButton': 'Add a new set of transformation',
          },
        },
      },
    },
  },
}
