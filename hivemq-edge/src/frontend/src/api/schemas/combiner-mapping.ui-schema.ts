import type { UiSchema } from '@rjsf/utils'
import i18nConfig from '@/config/i18n.config'
import { DataCombiningTableField } from '@/modules/Mappings/components/combiner/DataCombiningTableField'
import { EntityReferenceTableWidget } from '@/modules/Mappings/components/combiner/EntityReferenceTableWidget'
import { DataCombiningEditorField } from '@/modules/Mappings/components/combiner/DataCombiningEditorField'

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
      'ui:widget': EntityReferenceTableWidget,
    },
  },
  mappings: {
    items: {
      'ui:field': DataCombiningTableField,
      items: {
        'ui:field': DataCombiningEditorField,
        'ui:submitButtonOptions': {
          norender: true,
        },
        sources: {
          'ui:title': i18nConfig.t('combiner.schema.mappings.sources.title'),
          'ui:description': i18nConfig.t('combiner.schema.mappings.sources.description'),
          primary: {
            'ui:title': i18nConfig.t('combiner.schema.mappings.sources.primary.title'),
            'ui:description': i18nConfig.t('combiner.schema.mappings.sources.primary.description'),
          },
        },
        destination: {
          'ui:title': i18nConfig.t('combiner.schema.mappings.destination.title'),
          'ui:description': i18nConfig.t('combiner.schema.mappings.destination.description'),
          topic: {
            'ui:title': i18nConfig.t('combiner.schema.mappings.destination.topic.title'),
            'ui:description': i18nConfig.t('combiner.schema.mappings.destination.topic.description'),
          },
          schema: {
            'ui:title': i18nConfig.t('combiner.schema.mappings.destination.schema.title'),
            'ui:description': i18nConfig.t('combiner.schema.mappings.destination.schema.description'),
          },
        },
      },
    },
  },
}
