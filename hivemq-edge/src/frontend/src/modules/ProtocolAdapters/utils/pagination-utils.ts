import { Status } from '@/api/__generated__'

const STATUS_ERROR = Status.connectionStatus.ERROR
const STATUS_ORDER = {
  [Status.connectionStatus.ERROR]: 0,
  [Status.connectionStatus.CONNECTED]: 1,
  [Status.connectionStatus.DISCONNECTED]: 2,
  [Status.connectionStatus.STATELESS]: 3,
  [Status.connectionStatus.UNKNOWN]: 4,
}

export function compareStatus(rowA: Status.connectionStatus | undefined, rowB: Status.connectionStatus | undefined) {
  const a = STATUS_ORDER[rowA || STATUS_ERROR]
  const b = STATUS_ORDER[rowB || STATUS_ERROR]

  if (a > b) return 1
  if (a < b) return -1
  return 0
}
