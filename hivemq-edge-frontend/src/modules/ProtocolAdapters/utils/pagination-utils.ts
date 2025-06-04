import { Event, Status } from '@/api/__generated__'

const STATUS_ERROR = Status.connection.ERROR
const STATUS_ORDER = {
  [Status.connection.ERROR]: 0,
  [Status.connection.CONNECTED]: 1,
  [Status.connection.DISCONNECTED]: 2,
  [Status.connection.STATELESS]: 3,
  [Status.connection.UNKNOWN]: 4,
}

export function compareStatus(rowA: Status.connection | undefined, rowB: Status.connection | undefined) {
  const a = STATUS_ORDER[rowA || STATUS_ERROR]
  const b = STATUS_ORDER[rowB || STATUS_ERROR]

  if (a > b) return 1
  if (a < b) return -1
  return 0
}

const severityWeight = {
  [Event.severity.INFO]: 0,
  [Event.severity.WARN]: 1,
  [Event.severity.ERROR]: 2,
  [Event.severity.CRITICAL]: 3,
}

export function compareSeverity(rowA: Event.severity | undefined, rowB: Event.severity | undefined) {
  const a = rowA ? severityWeight[rowA] : -1
  const b = rowB ? severityWeight[rowB] : -1

  if (a > b) return 1
  if (a < b) return -1
  return 0
}
