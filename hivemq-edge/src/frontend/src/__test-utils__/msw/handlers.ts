import { handlers as useFrontendServices } from '@/api/hooks/useFrontendServices/__handlers__'
import { handlers as AuthHandlers } from '@/api/hooks/usePostAuthentication/__handlers__'
import { handlers as ConnectionStatusHandlers } from '@/api/hooks/useConnection/__handlers__'
import { handlers as BridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import { handlers as ProtocolAdapterHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as ListenerHandlers } from '@/api/hooks/useGateway/__handlers__'
import { handlers as TopicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'

import { handlers as DataHubDataPoliciesService } from '@/extensions/datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { handlers as DataHubBehaviorPoliciesService } from '@/extensions/datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import { handlers as DataHubSchemasService } from '@/extensions/datahub/api/hooks/DataHubSchemasService/__handlers__'
import { handlers as DataHubScriptsService } from '@/extensions/datahub/api/hooks/DataHubScriptsService/__handlers__'

import { handlers as ClientFilterHandlers } from '@/api/hooks/useClientSubscriptions/__handlers__'
import { deviceHandlers as DeviceHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { schemaHandlers } from '@/api/hooks/useDomainModel/__handlers__'
import { MQTTSample } from '@/hooks/usePrivateMqttClient/type.ts'

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

export const createHandlersWithMQTTClient = (
  onSampling?: (topicFilter: string) => Promise<MQTTSample[]> | undefined
) => {
  return [
    ...ClientFilterHandlers,
    ...DeviceHandlers,
    ...TopicFilterHandlers,
    // Domain & Schemas
    ...schemaHandlers(onSampling),
  ]
}
