import { useQuery } from '@tanstack/react-query'
import { ApiError, type JsonNode } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetSchemaForTopic = (topic: string, hasBeenStarted = false) => {
  const appClient = useHttpClient()

  return useQuery<JsonNode, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_PAYLOADS, topic, QUERY_KEYS.DISCOVERY_SCHEMAS],
    queryFn: () => appClient.payloadSampling.getSchemaForTopic(btoa(topic)),
    enabled: hasBeenStarted,
    retry: 1,
  })
}
