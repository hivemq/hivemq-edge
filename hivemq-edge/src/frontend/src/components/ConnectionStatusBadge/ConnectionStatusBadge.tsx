import { FC } from 'react'
import { Status } from '@/api/__generated__'
import { Badge } from '@chakra-ui/react'

const statusMapping = {
  [Status.connectionStatus.ERROR]: { text: 'error', color: 'status.error' },
  [Status.connectionStatus.CONNECTED]: { text: 'Connected', color: 'status.connected' },
  [Status.connectionStatus.DISCONNECTED]: { text: 'Disconnected', color: 'status.disconnected' },
  [Status.connectionStatus.STATELESS]: { text: 'Stateless', color: 'status.connected' },
  [Status.connectionStatus.UNKNOWN]: { text: 'Disconnecting', color: 'status.disconnecting' },
}

interface ConnectionStatusBadgeProps {
  status?: Status.connectionStatus
}

const ConnectionStatusBadge: FC<ConnectionStatusBadgeProps> = ({ status }) => {
  return (
    <Badge
      variant="subtle"
      colorScheme={statusMapping[status || 'ERROR'].color}
      borderRadius={15}
      data-testid={'connection-status'}
    >
      {statusMapping[status || 'ERROR'].text}
    </Badge>
  )
}

export default ConnectionStatusBadge
