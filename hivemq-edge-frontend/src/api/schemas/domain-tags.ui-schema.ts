import type { UiSchema } from '@rjsf/utils'
import { TagTableField } from '../../modules/Device/components/TagTableField'

/* istanbul ignore next -- @preserve */
export const tagListUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },

  items: {
    'ui:field': TagTableField,
    items: {
      'ui:order': ['name', 'description', '*'],
      'ui:collapsable': {
        titleKey: 'name',
      },
      protocolId: {
        'ui:widget': 'hidden',
      },
      'ui:submitButtonOptions': {
        norender: true,
      },
    },
  },
}
