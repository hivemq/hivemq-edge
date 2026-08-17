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
  // Shares the disconnected colour rather than getting one of its own: at a glance the colour answers "is
  // data flowing", and for a connecting adapter the answer is no. The label carries the difference. A
  // distinct in-progress treatment (a pulsing badge, say) is a design decision, not one to make here.
  [Status.connection.CONNECTING]: { text: 'CONNECTING', color: 'status.disconnected' },
  [Status.connection.STATELESS]: { text: 'STATELESS', color: 'status.stateless' },
} as const

/**
 * What to show for a status this build does not know about.
 *
 * `Status.connection` is generated from the API spec and grows: `CONNECTING` was added to it after this
 * component was written, and until then an adapter publishing it reached the lookup below as an absent key —
 * `undefined`, and a `TypeError` on the next property access. A badge is not worth a blank page, and a
 * frontend older than the Edge it talks to is an ordinary situation during a rolling upgrade.
 */
const UNRECOGNISED = { text: 'UNKNOWN', color: 'status.unknown' } as const

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
    ] ?? UNRECOGNISED

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
