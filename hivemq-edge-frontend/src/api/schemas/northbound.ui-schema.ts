import type { UiSchema } from '@rjsf/utils'
import { registerEntitySelectWidget } from '@/components/rjsf/Widgets/EntitySelectWidget.tsx'
import { CustomFormat } from '@/api/types/json-schema.ts'

/* istanbul ignore next -- @preserve */
export const northboundMappingListUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },

  items: {
    'ui:title': 'List of northbound mappings',
    'ui:description': 'The list of all the mappings delivering messages from this adapter onto the Edge',
    items: {
      // The two method-call response settings go last: they are inert unless the southbound mapping is
      // configured for method calls, so they should not sit among the settings that always apply.
      'ui:order': ['tagName', 'topic', '*', 'writeCorrelationData', 'useRequestResponseTopic'],
      'ui:collapsable': {
        titleKey: 'tagName',
        name: 'Mapping',
      },
      tagName: {
        'ui:widget': registerEntitySelectWidget(CustomFormat.MQTT_TAG),
      },
      topic: {
        'ui:widget': registerEntitySelectWidget(CustomFormat.MQTT_TOPIC),
        'ui:options': {
          create: true,
        },
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
    },
  },
}
