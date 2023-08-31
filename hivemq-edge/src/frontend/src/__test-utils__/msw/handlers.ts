import { handlers as AuthHandlers } from '../../api/hooks/usePostAuthentication/__handlers__'
import { handlers as ConnectionStatusHandlers } from '../../api/hooks/useConnection/__handlers__'
import { handlers as BridgeHandlers } from '../../api/hooks/useGetBridges/__handlers__'
import { handlers as ProtocolAdapterHandlers } from '../../api/hooks/useProtocolAdapters/__handlers__'
import { handlers as ListenerHandlers } from '../../api/hooks/useGateway/__handlers__'

export const handlers = [
  ...ListenerHandlers,
  ...AuthHandlers,
  ...ConnectionStatusHandlers,
  ...BridgeHandlers,
  ...ProtocolAdapterHandlers,
]
