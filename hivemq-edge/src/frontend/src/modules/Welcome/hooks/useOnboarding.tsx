import { useTranslation } from 'react-i18next'
import { IoLinkOutline } from 'react-icons/io5'
import { GoLinkExternal } from 'react-icons/go'

import { OnboardingTask } from '@/modules/Welcome/types.ts'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'

export const useOnboarding = (): OnboardingTask[] => {
  const { t } = useTranslation()
  const { data } = useGetConfiguration()

  if (!data) return []

  return [
    {
      header: t('welcome.onboarding.connectDevice.header'),
      sections: [
        {
          title: t('welcome.onboarding.connectDevice.protocolAdapter.title'),
          label: t('welcome.onboarding.connectDevice.protocolAdapter.label'),
          to: '/protocol-adapters',
          leftIcon: <IoLinkOutline />,
        },
      ],
    },
    {
      header: t('welcome.onboarding.connectEnterprise.header'),
      sections: [
        {
          title: t('welcome.onboarding.connectEnterprise.bridge.title'),
          label: t('welcome.onboarding.connectEnterprise.bridge.label'),
          to: '/mqtt-bridges',
          leftIcon: <IoLinkOutline />,
        },
      ],
    },
    {
      header: data?.cloudLink?.displayText as string,
      sections: [
        {
          title: data?.cloudLink?.description as string,
          label: data?.cloudLink?.displayText as string,
          to: data?.cloudLink?.url as string,
          isExternal: true,
          leftIcon: <GoLinkExternal />,
        },
      ],
    },
  ]
}
