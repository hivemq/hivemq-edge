import type { UiSchema } from '@rjsf/utils'

import i18nConfig from '@/config/i18n.config.ts'

import { CustomFormat } from '@/api/types/json-schema.ts'
import { registerEntitySelectWidget } from '@/components/rjsf/Widgets/EntitySelectWidget.tsx'
import SchemaWidget from '@/components/rjsf/Widgets/SchemaWidget.tsx'
import MappingTargetWidget from '@/modules/Pulse/components/widgets/MappingTargetWidget.tsx'

/* istanbul ignore next -- @preserve */
export const managedAssetUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },

  mapping: {
    'ui:readonly': true,
    'ui:title': i18nConfig.t('pulse.assets.viewer.mapping.title'),
    'ui:description': i18nConfig.t('pulse.assets.viewer.mapping.description'),

    status: {
      'ui:readonly': true,
      'ui:title': i18nConfig.t('pulse.assets.viewer.mapping.status.title'),
      'ui:description': i18nConfig.t('pulse.assets.viewer.mapping.status.description'),
    },

    mappingId: {
      'ui:readonly': true,
      'ui:widget': MappingTargetWidget,
      'ui:title': i18nConfig.t('pulse.assets.viewer.mapping.mappingId.title'),
      'ui:description': i18nConfig.t('pulse.assets.viewer.mapping.mappingId.description'),
    },
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
      title: i18nConfig.t('pulse.assets.viewer.tabs.config'),
      properties: ['id', 'name', 'description'],
    },
    {
      id: 'destinationTab',
      title: i18nConfig.t('pulse.assets.viewer.tabs.destination'),
      properties: ['topic', 'schema'],
    },
    {
      id: 'mappingsTab',
      title: i18nConfig.t('pulse.assets.viewer.tabs.mapping'),
      properties: ['mapping'],
    },
  ],
}
