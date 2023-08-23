import { UIGroup } from '@/modules/ProtocolAdapters/types.ts'
import { useTranslation } from 'react-i18next'
import { UiSchema } from '@rjsf/utils'

const useGetUiSchema = (isNewAdapter = true) => {
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

  const uiSchema: UiSchema = {
    'ui:groups': groups,

    'ui:submitButtonOptions': {
      norender: true,
    },

    // TODO[NVL] Make sure only the top-level id is disabled. See 16318
    id: {
      'ui:disabled': !isNewAdapter,
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
