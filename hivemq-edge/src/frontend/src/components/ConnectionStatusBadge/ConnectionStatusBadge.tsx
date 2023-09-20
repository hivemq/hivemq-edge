import { FC } from 'react'
import { Status } from '@/api/__generated__'
import { Badge } from '@chakra-ui/react'

const statusMapping = {
  [Status.connection.ERROR]: { text: 'error', color: 'status.error' },
  [Status.connection.CONNECTED]: { text: 'Connected', color: 'status.connected' },
  [Status.connection.DISCONNECTED]: { text: 'Disconnected', color: 'status.disconnected' },
  [Status.connection.STATELESS]: { text: 'Stateless', color: 'status.connected' },
  [Status.connection.UNKNOWN]: { text: 'Disconnecting', color: 'status.disconnecting' },
}

interface ConnectionStatusBadgeProps {
  status?: Status.connection
}

const ConnectionStatusBadge: FC<ConnectionStatusBadgeProps> = ({ status }) => {
  console.log('XXXXX st', status)
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
