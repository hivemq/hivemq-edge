import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError, type TagSchema } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

// TODO[NVL] Should be a single tag or a multiple schema return; see https://github.com/hivemq/hivemq-edge/pull/564#discussion_r1768004566
export const useGetTopicSchemas = (topics: Array<string>) => {
  const appClient = useHttpClient()

  return useQuery<TagSchema, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_TAGS, topics, QUERY_KEYS.DISCOVERY_TOPICS],
    queryFn: () => appClient.domain.getTopicSchemas(topics),
    enabled: topics.length > 0,
  })
}
