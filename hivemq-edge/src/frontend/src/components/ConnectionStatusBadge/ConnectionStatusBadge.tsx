import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@chakra-ui/react'

import { Status } from '@/api/__generated__'
import { statusMapping } from '@/modules/Workspace/utils/adapter.utils.ts'

interface ConnectionStatusBadgeProps {
  status?: Status
}

const ConnectionStatusBadge: FC<ConnectionStatusBadgeProps> = ({ status }) => {
  const { t } = useTranslation()

  const mapping =
    statusMapping[
      status?.runtime === Status.runtime.STOPPED
        ? Status.runtime.STOPPED
        : status?.connection || Status.connection.UNKNOWN
    ]

  return (
    <Badge variant="subtle" colorScheme={mapping.color} borderRadius={15} data-testid="connection-status">
      {t('hivemq.connection.status', { context: mapping.text })}
    </Badge>
  )
}

export default ConnectionStatusBadge
