import { handlers as useFrontendServices } from '@/api/hooks/useFrontendServices/__handlers__'
import { handlers as AuthHandlers } from '@/api/hooks/usePostAuthentication/__handlers__'
import { handlers as ConnectionStatusHandlers } from '@/api/hooks/useConnection/__handlers__'
import { handlers as BridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import { handlers as ProtocolAdapterHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as ListenerHandlers } from '@/api/hooks/useGateway/__handlers__'

import { handlers as DataHubDataPoliciesService } from '@/extensions/datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { handlers as DataHubBehaviorPoliciesService } from '@/extensions/datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import { handlers as DataHubSchemasService } from '@/extensions/datahub/api/hooks/DataHubSchemasService/__handlers__'
import { handlers as DataHubScriptsService } from '@/extensions/datahub/api/hooks/DataHubScriptsService/__handlers__'

export const handlers = [
  ...useFrontendServices,
  ...ListenerHandlers,
  ...AuthHandlers,
  ...ConnectionStatusHandlers,
  ...BridgeHandlers,
  ...ProtocolAdapterHandlers,
  // Datahub extension
  ...DataHubDataPoliciesService,
  ...DataHubBehaviorPoliciesService,
  ...DataHubSchemasService,
  ...DataHubScriptsService,
]
