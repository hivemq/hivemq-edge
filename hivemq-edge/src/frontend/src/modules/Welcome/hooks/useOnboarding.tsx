import { useTranslation } from 'react-i18next'
import { IoLinkOutline } from 'react-icons/io5'
import { GoLinkExternal } from 'react-icons/go'

import { OnboardingTask } from '@/modules/Welcome/types.ts'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'
import { ApiError } from '@/api/__generated__'

export interface OnboardingFetchType {
  data?: OnboardingTask[]
  error?: ApiError | null
}

export const useOnboarding = (): OnboardingFetchType => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetConfiguration()

  const cloud: OnboardingTask = {
    isLoading: isLoading,
    header: t('welcome.onboarding.connectCloud.header'),
    sections: [
      {
        title: t('welcome.onboarding.connectCloud.section.title'),
        label: t('welcome.onboarding.connectCloud.section.label'),
        to: data?.cloudLink?.url as string,
        isExternal: true,
        leftIcon: <GoLinkExternal />,
      },
    ],
  }

  const tasks: OnboardingTask[] = [
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

  if (isLoading || !isError) tasks.push(cloud)

  return {
    error,
    data: tasks,
  }
}
