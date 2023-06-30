import { FC } from 'react'
import { ConnectionStatus } from '@/api/__generated__'
import { Badge } from '@chakra-ui/react'

const statusMapping = {
  ERROR: { text: 'error', color: 'red' },
  [ConnectionStatus.status.CONNECTING]: { text: 'Connecting', color: 'gray' },
  [ConnectionStatus.status.DISCONNECTING]: { text: 'Disconnecting', color: 'gray' },
  [ConnectionStatus.status.CONNECTED]: { text: 'Connected', color: 'green' },
  [ConnectionStatus.status.DISCONNECTED]: { text: 'Disconnected', color: 'yellow' },
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
