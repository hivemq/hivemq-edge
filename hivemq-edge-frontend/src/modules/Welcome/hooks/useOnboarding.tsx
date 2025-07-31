import { useTranslation } from 'react-i18next'
import { IoLinkOutline } from 'react-icons/io5'
import { GoLinkExternal } from 'react-icons/go'

import type { ApiError } from '@/api/__generated__'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'
import { PulseAgentIcon } from '@/components/Icons/PulseAgentIcon.tsx'
import type { OnboardingTask } from '@/modules/Welcome/types.ts'
import { ActivationPanel } from '@/modules/Pulse/components/ActivationPanel.tsx'

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

  const pulse: OnboardingTask = {
    isLoading: isLoading,
    header: t('welcome.onboarding.pulse.header'),
    sections: [
      {
        title: t('welcome.onboarding.pulse.section.activate.title'),
        label: t('welcome.onboarding.pulse.section.activate.label'),
        content: <ActivationPanel />,
        leftIcon: <PulseAgentIcon />,
      },
      {
        title: t('welcome.onboarding.pulse.section.assets.title'),
        label: t('welcome.onboarding.pulse.section.assets.label'),
        to: '/pulse-assets',
        leftIcon: <PulseAgentIcon />,
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

  if (isLoading || !isError) tasks.push(cloud, pulse)

  return {
    error,
    data: tasks,
  }
}
