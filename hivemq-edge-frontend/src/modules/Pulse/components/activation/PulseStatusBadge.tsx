import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, SkeletonCircle } from '@chakra-ui/react'

import { PulseStatus } from '@/api/__generated__'

const statusMapping = {
  [PulseStatus.activation.ERROR]: { text: PulseStatus.activation.ERROR, color: 'status.error' },
  [PulseStatus.activation.DEACTIVATED]: {
    text: PulseStatus.activation.DEACTIVATED,
    color: 'status.unknown',
  },
  [PulseStatus.activation.ACTIVATED]: { text: PulseStatus.activation.ACTIVATED, color: 'status.connected' },
  [PulseStatus.runtime.CONNECTED]: { text: PulseStatus.runtime.CONNECTED, color: 'status.connected' },
  [PulseStatus.runtime.DISCONNECTED]: {
    text: PulseStatus.runtime.DISCONNECTED,
    color: 'status.disconnected',
  },
} as const

interface ConnectionStatusBadgeProps {
  status: PulseStatus
  skeleton?: boolean
}

const PulseStatusBadge: FC<ConnectionStatusBadgeProps> = ({ status, skeleton = false }) => {
  const { t } = useTranslation()

  const mapping =
    statusMapping[status.activation === PulseStatus.activation.ACTIVATED ? status.runtime : status.activation]

  if (skeleton)
    return (
      <SkeletonCircle
        size="8"
        startColor={`${mapping.color}.300`}
        endColor={`${mapping.color}.500`}
        data-activation={status.activation}
        data-runtime={status.runtime}
        data-testid="pulse-status"
      />
    )

  return (
    <Badge
      variant="subtle"
      colorScheme={mapping.color}
      borderRadius={15}
      data-testid="pulse-status"
      data-activation={status.activation}
      data-runtime={status.runtime}
    >
      {t('pulse.status', { context: mapping.text })}
    </Badge>
  )
}

export default PulseStatusBadge
