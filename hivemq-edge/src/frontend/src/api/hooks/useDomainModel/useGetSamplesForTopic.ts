import { useQuery } from '@tanstack/react-query'
import { ApiError, type PayloadSampleList } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetSamplesForTopic = (topic: string, hasBeenStarted = false) => {
  const appClient = useHttpClient()

  return useQuery<PayloadSampleList, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_PAYLOADS, topic],
    queryFn: () => appClient.payloadSampling.getSamplesForTopic(encodeURIComponent(topic)),
    enabled: hasBeenStarted,
    retry: 1,
  })
}
