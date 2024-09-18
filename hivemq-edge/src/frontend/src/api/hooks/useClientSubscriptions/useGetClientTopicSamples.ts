import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError, type ClientTopicList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetClientTopicSamples = (queryTime?: number) => {
  const appClient = useHttpClient()

  return useQuery<ClientTopicList, ApiError>({
    // eslint-disable-next-line @tanstack/query/exhaustive-deps
    queryKey: [QUERY_KEYS.CLIENTS, QUERY_KEYS.DISCOVERY_TOPICS],
    queryFn: () => appClient.client.getClientTopics(queryTime),
  })
}
