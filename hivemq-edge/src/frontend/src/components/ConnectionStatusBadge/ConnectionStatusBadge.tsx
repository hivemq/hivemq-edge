import { FC } from 'react'
import { ConnectionStatus } from '@/api/__generated__'
import { Badge } from '@chakra-ui/react'

const statusMapping = {
  ERROR: { text: 'error', color: 'status.error' },
  [ConnectionStatus.status.CONNECTING]: { text: 'Connecting', color: 'status.connecting' },
  [ConnectionStatus.status.DISCONNECTING]: { text: 'Disconnecting', color: 'status.disconnecting' },
  [ConnectionStatus.status.CONNECTED]: { text: 'Connected', color: 'status.connected' },
  [ConnectionStatus.status.DISCONNECTED]: { text: 'Disconnected', color: 'status.disconnected' },
}

interface ConnectionStatusBadgeProps {
  status?: ConnectionStatus.status
}

const ConnectionStatusBadge: FC<ConnectionStatusBadgeProps> = ({ status }) => {
  return (
    <Badge variant="solid" colorScheme={statusMapping[status || 'ERROR'].color} borderRadius={15}>
      {statusMapping[status || 'ERROR'].text}
    </Badge>
  )
}

export default ConnectionStatusBadge
