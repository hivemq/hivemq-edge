import { useTranslation } from 'react-i18next'
import { IoLinkOutline } from 'react-icons/io5'
import { OnboardingTask } from '@/modules/Welcome/types.ts'

export const useOnboarding = (): OnboardingTask[] => {
  const { t } = useTranslation()

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
  ]
}
