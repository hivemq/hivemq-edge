import type { UiSchema } from '@rjsf/utils'

import i18nConfig from '@/config/i18n.config.ts'

/* istanbul ignore next -- @preserve */
export const bridgeUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },
  'ui:order': ['id', 'host', 'port', '*', 'loopPreventionEnabled', 'loopPreventionHopCount'],
  'ui:tabs': [
    {
      id: 'bridgeConnection',
      title: i18nConfig.t('bridge.schema.tabs.connection'),
      properties: ['id', 'host', 'port', 'clientId', 'username', 'password'],
    },
    {
      id: 'bridgeBroker',
      title: i18nConfig.t('bridge.schema.tabs.broker'),
      properties: ['cleanStart', 'keepAlive', 'loopPreventionEnabled', 'loopPreventionHopCount', 'sessionExpiry'],
    },
    {
      id: 'bridgeSubscription',
      title: i18nConfig.t('bridge.schema.tabs.subscriptions'),
      properties: ['localSubscriptions', 'remoteSubscriptions'],
    },

    {
      id: 'bridgeSecurity',
      title: i18nConfig.t('bridge.schema.tabs.security'),
      properties: ['tlsConfiguration'],
    },
    {
      id: 'bridgeWebSocket',
      title: i18nConfig.t('bridge.schema.tabs.webSocket'),
      properties: ['websocketConfiguration'],
    },
    {
      id: 'bridgePersist',
      title: i18nConfig.t('bridge.schema.tabs.persistence'),
      properties: ['persist'],
    },
  ],

  id: {
    'ui:title': i18nConfig.t('bridge.options.id.label'),
    'ui:placeholder': i18nConfig.t('bridge.options.id.placeholder'),
    'ui:description': i18nConfig.t('bridge.options.id.helper'),
  },
  host: {
    'ui:title': i18nConfig.t('bridge.connection.host'),
  },
  port: {
    'ui:title': i18nConfig.t('bridge.connection.port'),
  },
  username: {
    'ui:title': i18nConfig.t('bridge.connection.username'),
  },
  password: {
    'ui:title': i18nConfig.t('bridge.connection.password'),
  },

  cleanStart: {
    'ui:title': i18nConfig.t('bridge.options.cleanStart.label'),
    'ui:description': i18nConfig.t('bridge.options.cleanStart.helper'),
  },

  loopPreventionEnabled: {
    'ui:widget': 'radio',
  },
  tlsConfiguration: {
    'ui:order': ['enabled', '*'],
  },

  websocketConfiguration: {
    'ui:order': ['enabled', '*'],
  },
  status: {
    // Status is not part of the editor
    'ui:widget': 'hidden',
  },
}
