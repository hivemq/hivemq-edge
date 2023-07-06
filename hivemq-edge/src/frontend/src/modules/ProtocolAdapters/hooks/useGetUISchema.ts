import { UIGroup } from '@/modules/ProtocolAdapters/types.ts'
import { useTranslation } from 'react-i18next'

const useGetUiSchema = () => {
  const { t } = useTranslation()

  const groups: UIGroup[] = [
    {
      id: 'coreFields',
      title: t('protocolAdapter.uiSchema.groups.coreFields'),
      children: ['id', 'port', 'host', 'uri', 'pollingIntervalMillis'],
    },
    { id: 'subFields', title: 'Subscription', children: ['subscriptions'] },
    {
      id: 'security',
      title: t('protocolAdapter.uiSchema.groups.security'),
      children: ['security', 'tls'],
    },
    {
      id: 'publishing',
      title: t('protocolAdapter.uiSchema.groups.publishing'),
      children: ['maxPollingErrorsBeforeRemoval', 'publishChangedDataOnly', 'publishingInterval'],
    },
    {
      id: 'authentication',
      title: t('protocolAdapter.uiSchema.groups.authentication'),
      children: ['auth'],
    },
  ]

  const uiSchema = {
    'ui:groups': groups,

    'ui:submitButtonOptions': {
      norender: true,
    },
    // pollingIntervalMillis: {
    //   'ui:widget': 'range',
    // },
    port: {
      'ui:widget': 'updown',
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
