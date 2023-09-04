import { ConnectionStatus } from '@/api/__generated__'

const STATUS_ERROR = 'STATUS_ERROR'
const STATUS_ORDER = {
  [STATUS_ERROR]: 0,
  [ConnectionStatus.status.CONNECTED]: 1,
  [ConnectionStatus.status.DISCONNECTED]: 2,
  [ConnectionStatus.status.CONNECTING]: 3,
  [ConnectionStatus.status.DISCONNECTING]: 4,
}

export function compareStatus(rowA: ConnectionStatus.status | undefined, rowB: ConnectionStatus.status | undefined) {
  const a = STATUS_ORDER[rowA || STATUS_ERROR]
  const b = STATUS_ORDER[rowB || STATUS_ERROR]

  if (a > b) return 1
  if (a < b) return -1
  return 0
}
