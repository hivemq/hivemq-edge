import { useTranslation } from 'react-i18next'
import { GoLinkExternal } from 'react-icons/go'
import { IoLinkOutline } from 'react-icons/io5'

import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'
import { PulseAgentIcon } from '@/components/Icons/PulseAgentIcon.tsx'
import { ActivationPanel } from '@/modules/Pulse/components/activation/ActivationPanel.tsx'
import type { OnboardingTask } from '@/modules/Welcome/types.ts'
import AssetMonitoringOnboardingTask from '@/modules/Pulse/components/assets/AssetMonitoringOnboardingTask.tsx'

export const useOnboarding = (): OnboardingTask[] => {
  const { t } = useTranslation()
  const { data: config, isLoading: isConfigLoading, error: configError } = useGetConfiguration()
  const {
    data: hasPulse,
    error: pulseError,
    isLoading: isPulseLoading,
  } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

  const cloud: OnboardingTask = {
    isLoading: isConfigLoading,
    error: configError,
    header: t('welcome.onboarding.connectCloud.header'),
    sections: [
      {
        title: t('welcome.onboarding.connectCloud.section.title'),
        label: t('welcome.onboarding.connectCloud.section.label'),
        to: config?.cloudLink?.url,
        isExternal: true,
        leftIcon: <GoLinkExternal />,
      },
    ],
  }

  const pulse: OnboardingTask = {
    isLoading: isPulseLoading,
    error: pulseError,
    header: t('welcome.onboarding.pulse.header'),
    sections: [
      {
        title: hasPulse
          ? t('welcome.onboarding.pulse.section.manage.title')
          : t('welcome.onboarding.pulse.section.activate.title'),
        label: t('welcome.onboarding.pulse.section.activate.label'),
        content: <ActivationPanel />,
        leftIcon: <PulseAgentIcon />,
      },
      ...(hasPulse
        ? [
            {
              title: t('welcome.onboarding.pulse.section.assets.title'),
              label: t('welcome.onboarding.pulse.section.assets.label'),
              to: '/pulse-assets',
              leftIcon: <PulseAgentIcon boxSize={6} />,
            },

            {
              title: t('pulse.onboarding.monitoring.task'),
              label: '',
              leftIcon: <PulseAgentIcon boxSize={6} />,
              content: <AssetMonitoringOnboardingTask />,
            },
          ]
        : []),
    ],
  }

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
    cloud,
    pulse,
  ]
}
