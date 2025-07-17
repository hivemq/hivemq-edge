import type { UiSchema } from '@rjsf/utils'
import ToggleWidget from '@/components/rjsf/Widgets/ToggleWidget.tsx'

import i18nConfig from '@/config/i18n.config.ts'

/**
 * TODO[NVL] Lots of repetitive patterns with i18n, should be refactored
 */
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
    'ui:widget': 'updown',
  },
  username: {
    'ui:title': i18nConfig.t('bridge.connection.username'),
    'ui:autocomplete': 'username',
  },
  password: {
    'ui:title': i18nConfig.t('bridge.connection.password'),
    'ui:autocomplete': 'current-password',
  },
  clientId: {
    'ui:title': i18nConfig.t('bridge.connection.clientId'),
  },

  cleanStart: {
    'ui:title': i18nConfig.t('bridge.options.cleanStart.label'),
    'ui:description': i18nConfig.t('bridge.options.cleanStart.helper'),
  },

  keepAlive: {
    'ui:title': i18nConfig.t('bridge.options.keepAlive.label'),
    'ui:description': i18nConfig.t('bridge.options.keepAlive.helper'),
    'ui:widget': 'updown',
  },

  sessionExpiry: {
    'ui:title': i18nConfig.t('bridge.options.sessionExpiry.label'),
    'ui:description': i18nConfig.t('bridge.options.sessionExpiry.helper'),
    'ui:widget': 'updown',
  },

  loopPreventionEnabled: {
    'ui:widget': ToggleWidget,
    'ui:title': i18nConfig.t('bridge.options.loopPrevention.label'),
    'ui:description': i18nConfig.t('bridge.options.loopPrevention.helper'),
  },

  loopPreventionHopCount: {
    'ui:title': i18nConfig.t('bridge.options.hopCount.label'),
    'ui:description': i18nConfig.t('bridge.options.hopCount.helper'),
    'ui:widget': 'updown',
  },

  localSubscriptions: {
    'ui:options': {
      addable: true,
      removable: true,
      orderable: false,
    },
    'ui:title': i18nConfig.t('bridge.subscription.type', { context: 'local', count: 4 }),
    'ui:description': i18nConfig.t('bridge.subscription.local.info'),
    items: {
      'ui:order': ['filters', 'destination', 'maxQoS', '*'],
      'ui:addButton': i18nConfig.t('bridge.subscription.add'),

      filters: {
        'ui:title': i18nConfig.t('bridge.subscription.filters.label'),
        'ui:description': i18nConfig.t('bridge.subscription.filters.helper'),
        items: {
          'ui:addButton': i18nConfig.t('bridge.subscription.filters.addButton'),
        },
      },
      destination: {
        'ui:title': i18nConfig.t('bridge.subscription.destination.label'),
        'ui:description': i18nConfig.t('bridge.subscription.destination.helper'),
      },
      customUserProperties: {
        'ui:title': i18nConfig.t('bridge.subscription.customUserProperties.label'),
        'ui:description': i18nConfig.t('bridge.subscription.customUserProperties.helper'),
        items: {
          'ui:addButton': i18nConfig.t('bridge.subscription.customUserProperties.actions.add'),

          key: {
            'ui:title': i18nConfig.t('bridge.subscription.customUserProperties.headers.key'),
          },
          value: {
            'ui:title': i18nConfig.t('bridge.subscription.customUserProperties.headers.value'),
          },
        },
      },
      excludes: {
        'ui:title': i18nConfig.t('bridge.subscription.excludes.label'),
        'ui:description': i18nConfig.t('bridge.subscription.excludes.helper'),
        items: {
          'ui:addButton': i18nConfig.t('bridge.subscription.excludes.addButton'),
          'ui:description': i18nConfig.t('bridge.subscription.excludes.helper'),
        },
      },
      preserveRetain: {
        'ui:title': i18nConfig.t('bridge.subscription.preserveRetain.label'),
        'ui:description': i18nConfig.t('bridge.subscription.preserveRetain.helper'),
      },
      queueLimit: {
        'ui:title': i18nConfig.t('bridge.subscription.queueLimit.label'),
        'ui:description': i18nConfig.t('bridge.subscription.queueLimit.helper'),
        'ui:widget': 'updown',
      },
    },
  },

  remoteSubscriptions: {
    'ui:options': {
      addable: true,
      removable: true,
      orderable: false,
    },
    'ui:title': i18nConfig.t('bridge.subscription.type', { context: 'remote', count: 4 }),
    'ui:description': i18nConfig.t('bridge.subscription.remote.info'),
    items: {
      'ui:order': ['filters', 'destination', 'maxQoS', '*'],
      'ui:addButton': i18nConfig.t('bridge.subscription.add'),

      filters: {
        'ui:title': i18nConfig.t('bridge.subscription.filters.label'),
        'ui:description': i18nConfig.t('bridge.subscription.filters.helper'),
        items: {
          'ui:addButton': i18nConfig.t('bridge.subscription.filters.addButton'),
        },
      },
      destination: {
        'ui:title': i18nConfig.t('bridge.subscription.destination.label'),
        'ui:description': i18nConfig.t('bridge.subscription.destination.helper'),
      },
      customUserProperties: {
        'ui:title': i18nConfig.t('bridge.subscription.customUserProperties.label'),
        'ui:description': i18nConfig.t('bridge.subscription.customUserProperties.helper'),
        items: {
          'ui:addButton': i18nConfig.t('bridge.subscription.customUserProperties.actions.add'),

          key: {
            'ui:title': i18nConfig.t('bridge.subscription.customUserProperties.headers.key'),
          },
          value: {
            'ui:title': i18nConfig.t('bridge.subscription.customUserProperties.headers.value'),
          },
        },
      },
      preserveRetain: {
        'ui:title': i18nConfig.t('bridge.subscription.preserveRetain.label'),
        'ui:description': i18nConfig.t('bridge.subscription.preserveRetain.helper'),
      },
    },
  },

  tlsConfiguration: {
    'ui:order': ['enabled', 'protocols', 'cipherSuites', '*'],
    'ui:title': i18nConfig.t('bridge.security.tlsConfiguration.label'),
    'ui:description': i18nConfig.t('bridge.security.tlsConfiguration.helper'),

    enabled: {
      'ui:widget': ToggleWidget,
      'ui:title': i18nConfig.t('bridge.security.enabled.label'),
      'ui:description': i18nConfig.t('bridge.security.enabled.helper'),
    },

    cipherSuites: {
      'ui:title': i18nConfig.t('bridge.security.cipherSuites.label'),
      'ui:description': i18nConfig.t('bridge.security.cipherSuites.helper'),
    },

    protocols: {
      'ui:title': i18nConfig.t('bridge.security.protocols.label'),
      'ui:description': i18nConfig.t('bridge.security.protocols.helper'),
    },

    keystorePath: {
      'ui:title': i18nConfig.t('bridge.security.keystorePath.label'),
      'ui:description': i18nConfig.t('bridge.security.keystorePath.helper'),
    },

    keystorePassword: {
      'ui:title': i18nConfig.t('bridge.security.keystorePassword.label'),
      'ui:description': i18nConfig.t('bridge.security.keystorePassword.helper'),
    },

    truststorePath: {
      'ui:title': i18nConfig.t('bridge.security.truststorePath.label'),
      'ui:description': i18nConfig.t('bridge.security.truststorePath.helper'),
    },

    truststorePassword: {
      'ui:title': i18nConfig.t('bridge.security.truststorePassword.label'),
      'ui:description': i18nConfig.t('bridge.security.truststorePassword.helper'),
    },
    privateKeyPassword: {
      'ui:title': i18nConfig.t('bridge.security.privateKeyPassword.label'),
      'ui:description': i18nConfig.t('bridge.security.privateKeyPassword.helper'),
    },

    handshakeTimeout: {
      'ui:widget': 'updown',
    },
  },

  websocketConfiguration: {
    'ui:order': ['enabled', '*'],
    'ui:title': i18nConfig.t('bridge.websocket.label'),
    'ui:description': i18nConfig.t('bridge.websocket.description'),

    enabled: {
      'ui:widget': ToggleWidget,
      'ui:title': i18nConfig.t('bridge.websocket.enabled.label'),
      'ui:description': i18nConfig.t('bridge.websocket.enabled.helper'),
    },

    serverPath: {
      'ui:title': i18nConfig.t('bridge.websocket.serverPath.label'),
      'ui:description': i18nConfig.t('bridge.websocket.serverPath.helper'),
    },

    subProtocol: {
      'ui:title': i18nConfig.t('bridge.websocket.subProtocol.label'),
      'ui:description': i18nConfig.t('bridge.websocket.subProtocol.helper'),
    },
  },

  persist: {
    'ui:title': i18nConfig.t('bridge.persistence.persist.label'),
    'ui:description': `${i18nConfig.t('bridge.persistence.persist.helper')}. ${i18nConfig.t('bridge.persistence.restartWarning')}`,
  },

  status: {
    // Status is not part of the editor
    'ui:widget': 'hidden',
  },
}
