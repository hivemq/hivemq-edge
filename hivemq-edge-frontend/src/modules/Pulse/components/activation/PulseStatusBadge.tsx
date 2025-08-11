import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, SkeletonCircle } from '@chakra-ui/react'

import { PulseStatus } from '@/api/__generated__'

const statusMapping = {
  [PulseStatus.activationStatus.ERROR]: { text: PulseStatus.activationStatus.ERROR, color: 'status.error' },
  [PulseStatus.activationStatus.DEACTIVATED]: {
    text: PulseStatus.activationStatus.DEACTIVATED,
    color: 'status.unknown',
  },
  [PulseStatus.activationStatus.ACTIVATED]: { text: PulseStatus.activationStatus.ACTIVATED, color: 'status.connected' },
  [PulseStatus.runtimeStatus.CONNECTED]: { text: PulseStatus.runtimeStatus.CONNECTED, color: 'status.connected' },
  [PulseStatus.runtimeStatus.DISCONNECTED]: {
    text: PulseStatus.runtimeStatus.DISCONNECTED,
    color: 'status.disconnected',
  },
}

interface ConnectionStatusBadgeProps {
  status: PulseStatus
  skeleton?: boolean
}

const PulseStatusBadge: FC<ConnectionStatusBadgeProps> = ({ status, skeleton = false }) => {
  const { t } = useTranslation()

  const mapping =
    statusMapping[
      status.activation.status === PulseStatus.activationStatus.ACTIVATED
        ? status.runtime.status
        : status.activation.status
    ]

  if (skeleton)
    return (
      <SkeletonCircle
        size="8"
        startColor={`${mapping.color}.300`}
        endColor={`${mapping.color}.500`}
        data-activation={status.activation.status}
        data-runtime={status.runtime.status}
        data-testid="pulse-status"
      />
    )

  return (
    <Badge
      variant="subtle"
      colorScheme={mapping.color}
      borderRadius={15}
      data-testid="pulse-status"
      data-activation={status.activation.status}
      data-runtime={status.runtime.status}
    >
      {t('pulse.status', { context: mapping.text })}
    </Badge>
  )
}

export default PulseStatusBadge
