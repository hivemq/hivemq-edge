import type { UiSchema } from '@rjsf/utils'
import i18nConfig from '@/config/i18n.config'

/* istanbul ignore next -- @preserve */
export const combinerMappingUiSchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },
  'ui:tabs': [
    {
      id: 'Combiner',
      title: 'Combiner',
      properties: ['id', 'name', 'description'],
    },
    {
      id: 'Sources',
      title: 'Sources',
      properties: ['sources'],
    },
    {
      id: 'Mappings',
      title: 'Mappings',
      properties: ['mappings'],
    },
  ],

  'ui:description': i18nConfig.t('A short description for the combiner'),

  id: {
    'ui:title': 'Unique id',
    'ui:readonly': true,
    // 'ui:widget': 'hidden',
  },
  description: {
    'ui:widget': 'textarea',
    'ui:description': i18nConfig.t('A short description for the combiner'),
  },
  sources: {
    // 'ui:widget': 'hidden',
  },
  mappings: {
    // 'ui:widget': 'hidden',
  },
}
