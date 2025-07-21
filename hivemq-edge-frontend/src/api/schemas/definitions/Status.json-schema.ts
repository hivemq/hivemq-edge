import type { RJSFSchema } from '@rjsf/utils'
import { Status } from '@/api/__generated__'

/* istanbul ignore next -- @preserve */
export const StatusConnection: RJSFSchema = {
  type: 'string',
  title: 'Connection',
  description: 'A mandatory connection status field',
  enum: [
    Status.connection.CONNECTED,
    Status.connection.DISCONNECTED,
    Status.connection.STATELESS,
    Status.connection.UNKNOWN,
    Status.connection.ERROR,
  ],
  default: Status.connection.CONNECTED,
}

/* istanbul ignore next -- @preserve */
export const StatusRuntime: RJSFSchema = {
  type: 'string',
  title: 'Runtime',
  description: 'A object status field',
  enum: [Status.runtime.STARTED, Status.runtime.STOPPED],
  default: Status.runtime.STARTED,
}
