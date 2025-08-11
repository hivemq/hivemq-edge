import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, SkeletonCircle } from '@chakra-ui/react'

import { Status } from '@/api/__generated__'

const statusMapping = {
  [Status.runtime.STOPPED]: { text: 'STOPPED', color: 'status.error' },
  [Status.connection.ERROR]: { text: 'ERROR', color: 'status.error' },
  [Status.connection.UNKNOWN]: { text: 'UNKNOWN', color: 'status.unknown' },
  [Status.connection.CONNECTED]: { text: 'CONNECTED', color: 'status.connected' },
  [Status.connection.DISCONNECTED]: { text: 'DISCONNECTED', color: 'status.disconnected' },
  [Status.connection.STATELESS]: { text: 'STATELESS', color: 'status.stateless' },
}

interface ConnectionStatusBadgeProps {
  status?: Status
  skeleton?: boolean
}

const ConnectionStatusBadge: FC<ConnectionStatusBadgeProps> = ({ status, skeleton = false }) => {
  const { t } = useTranslation()

  const mapping =
    statusMapping[
      status?.runtime === Status.runtime.STOPPED
        ? Status.runtime.STOPPED
        : status?.connection || Status.connection.UNKNOWN
    ]

  if (skeleton)
    return (
      <SkeletonCircle
        size="8"
        startColor={`${mapping.color}.300`}
        endColor={`${mapping.color}.500`}
        aria-label={mapping.text}
      />
    )

  return (
    <Badge variant="subtle" colorScheme={mapping.color} borderRadius={15} data-testid="connection-status">
      {t('hivemq.connection.status', { context: mapping.text })}
    </Badge>
  )
}

export default ConnectionStatusBadge
