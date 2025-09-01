import type { UiSchema } from '@rjsf/utils'

import i18nConfig from '@/config/i18n.config.ts'

import { CustomFormat } from '@/api/types/json-schema.ts'
import { registerEntitySelectWidget } from '@/components/rjsf/Widgets/EntitySelectWidget.tsx'
import SchemaWidget from '@/components/rjsf/Widgets/SchemaWidget.tsx'

/* istanbul ignore next -- @preserve */
export const managedAssetUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },

  mapping: {
    'ui:readonly': true,
  },

  schema: {
    'ui:widget': SchemaWidget,
  },

  topic: {
    'ui:widget': registerEntitySelectWidget(CustomFormat.MQTT_TOPIC),
    'ui:options': {
      create: false,
    },
  },

  'ui:tabs': [
    {
      id: 'assetTab',
      title: i18nConfig.t('Configuration'),
      properties: ['id', 'name', 'description'],
    },
    {
      id: 'destinationTab',
      title: i18nConfig.t('Destination'),
      properties: ['topic', 'schema'],
    },
    {
      id: 'mappingsTab',
      title: i18nConfig.t('Mappings'),
      properties: ['mapping'],
    },
  ],
}
