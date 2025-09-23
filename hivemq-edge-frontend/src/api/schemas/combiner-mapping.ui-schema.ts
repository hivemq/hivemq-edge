import type { UiSchema } from '@rjsf/utils'
import { DataCombiningTableField } from '@/modules/Mappings/combiner/DataCombiningTableField'
import { EntityReferenceTableWidget } from '@/modules/Mappings/combiner/EntityReferenceTableWidget'
import { DataCombiningEditorField } from '@/modules/Mappings/combiner/DataCombiningEditorField'

import i18nConfig from '@/config/i18n.config'

export const combinerMappingUiSchema = (isAssetManager = false, initTab?: string): UiSchema => {
  const context = isAssetManager ? 'ASSET_MAPPER' : 'COMBINER'
  const t = (key: string) => i18nConfig.t(key, { ns: 'schemas', context })

  return {
    'ui:submitButtonOptions': {
      norender: true,
    },
    'ui:initTab': initTab,
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
        title: isAssetManager
          ? i18nConfig.t('combiner.schema.tabs.assetTabs')
          : i18nConfig.t('combiner.schema.tabs.mappingsTab'),
        properties: ['mappings'],
      },
    ],

    'ui:description': t('Combiner.description'),

    id: {
      'ui:title': t('Combiner.id.title'),
      'ui:description': t('Combiner.id.description'),
      'ui:readonly': true,
    },
    name: {
      'ui:title': t('Combiner.name.title'),
      'ui:description': t('Combiner.name.description'),
    },
    description: {
      'ui:widget': 'textarea',
      'ui:title': t('Combiner.description.title'),
      'ui:description': t('Combiner.description.description'),
    },
    sources: {
      'ui:title': t('Combiner.sources.title'),
      'ui:description': t('Combiner.sources.description'),
      items: {
        'ui:widget': EntityReferenceTableWidget,
      },
    },
    mappings: {
      'ui:title': t('Combiner.mappings.title'),
      'ui:description': t('Combiner.mappings.description'),
      items: {
        'ui:field': DataCombiningTableField,
        items: {
          'ui:field': DataCombiningEditorField,
          'ui:submitButtonOptions': {
            norender: true,
          },
          sources: {
            'ui:title': t('Combiner.mappings.items.sources.title'),
            'ui:description': t('Combiner.mappings.items.sources.description'),
            primary: {
              'ui:title': t('Combiner.mappings.items.sources.primary.title'),
              'ui:description': t('Combiner.mappings.items.sources.primary.description'),
            },
          },
          destination: {
            'ui:title': t('Combiner.mappings.items.destination.title'),
            'ui:description': t('Combiner.mappings.items.destination.description'),
            topic: {
              'ui:title': t('Combiner.mappings.items.destination.topic.title'),
              'ui:description': t('Combiner.mappings.items.destination.topic.description'),
            },
            schema: {
              'ui:title': t('Combiner.mappings.items.destination.schema.title'),
              'ui:description': t('Combiner.mappings.items.destination.schema.description'),
            },
          },
        },
      },
    },
  }
}
