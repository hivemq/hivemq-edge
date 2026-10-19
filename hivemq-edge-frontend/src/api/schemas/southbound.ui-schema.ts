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
      // The three method-call settings go last and in dependency order: whether this mapping carries method
      // calls at all, then the two that refine it. A flat alphabetical order would put 'correlationData' first,
      // where it reads as a choice a user can make before deciding the mapping is a method mapping at all.
      'ui:order': ['topicFilter', 'tagName', '*', 'methodCalls', 'correlationData', 'responseTopic'],
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
