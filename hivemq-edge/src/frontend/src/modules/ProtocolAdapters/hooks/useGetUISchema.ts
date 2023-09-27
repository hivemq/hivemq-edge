import { UITab } from '@/modules/ProtocolAdapters/types.ts'
import { useTranslation } from 'react-i18next'
import { UiSchema } from '@rjsf/utils'

const useGetUiSchema = (isNewAdapter = true) => {
  const { t } = useTranslation()

  const tabs: UITab[] = [
    {
      id: 'coreFields',
      title: t('protocolAdapter.uiSchema.groups.coreFields'),
      properties: ['id', 'port', 'host', 'uri', 'url'],
    },
    {
      id: 'subFields',
      title: 'Subscription',
      properties: ['subscriptions'],
    },
    {
      id: 'security',
      title: t('protocolAdapter.uiSchema.groups.security'),
      properties: ['security', 'tls'],
    },
    {
      id: 'publishing',
      title: t('protocolAdapter.uiSchema.groups.publishing'),
      properties: [
        'maxPollingErrorsBeforeRemoval',
        'publishChangedDataOnly',
        'publishingInterval',
        'pollingIntervalMillis',
        'destination',
        'qos',
      ],
    },
    {
      id: 'authentication',
      title: t('protocolAdapter.uiSchema.groups.authentication'),
      properties: ['auth'],
    },
    {
      id: 'http',
      title: t('protocolAdapter.uiSchema.groups.http'),
      properties: [
        'httpRequestMethod',
        'httpRequestBodyContentType',
        'httpRequestBody',
        'httpHeaders',
        'httpConnectTimeout',
        'httpPublishSuccessStatusCodeOnly',
      ],
    },
  ]

  const uiSchema: UiSchema = {
    'ui:tabs': tabs,

    'ui:submitButtonOptions': {
      norender: true,
    },

    // TODO[NVL] Make sure only the top-level id is disabled. See 16318
    id: {
      'ui:disabled': !isNewAdapter,
    },
    port: {
      'ui:widget': 'updown',
    },
    httpRequestBody: {
      'ui:widget': 'textarea',
    },
    'ui:order': ['id', 'host', 'port', '*', 'subscriptions'],
    subscriptions: {
      items: {
        'ui:order': ['node', 'holding-registers', 'mqtt-topic', 'destination', 'qos', '*'],
      },
    },
    auth: {
      basic: {
        'ui:order': ['username', 'password', '*'],
      },
    },
  }

  return uiSchema
}

export default useGetUiSchema
