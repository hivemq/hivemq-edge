import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError, type TopicFilterList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListTopicFilters = () => {
  const appClient = useHttpClient()

  return useQuery<TopicFilterList, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS],
    queryFn: () => appClient.topicFilters.getTopicFilters(),
  })
}
