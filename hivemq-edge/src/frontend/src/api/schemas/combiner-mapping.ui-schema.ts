import type { UiSchema } from '@rjsf/utils'
import i18nConfig from '@/config/i18n.config'
import EntityReferenceTable from '@/modules/Mappings/components/combiner/EntityReferenceTable'

/* istanbul ignore next -- @preserve */
export const combinerMappingUiSchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },
  'ui:tabs': [
    {
      id: 'combinerTab',
      title: i18nConfig.t('combiner.schema.tabs.combinerTab'),
      properties: ['id', 'name', 'description'],
    },
    {
      id: 'sourcesTab',
      title: i18nConfig.t('combiner.schema.tabs.sourcesTab'),
      properties: ['sources'],
    },
    {
      id: 'mappingsTab',
      title: i18nConfig.t('combiner.schema.tabs.mappingsTab'),
      properties: ['mappings'],
    },
  ],

  'ui:description': i18nConfig.t('combiner.schema.description'),

  id: {
    'ui:title': 'Unique id',
    'ui:readonly': true,
    // 'ui:widget': 'hidden',
  },
  description: {
    'ui:widget': 'textarea',
    'ui:description': i18nConfig.t('combiner.schema.config.description'),
  },
  sources: {
    'ui:description': i18nConfig.t('combiner.schema.sources.description'),
    items: {
      'ui:widget': EntityReferenceTable,
    },
  },
  mappings: {
    // 'ui:widget': 'hidden',
  },
}
