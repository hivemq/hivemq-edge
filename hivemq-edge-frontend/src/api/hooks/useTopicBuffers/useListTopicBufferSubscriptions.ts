import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import type { ApiError, TopicBufferSubscriptionList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListTopicBufferSubscriptions = () => {
  const appClient = useHttpClient()

  return useQuery<TopicBufferSubscriptionList, ApiError>({
    queryKey: [QUERY_KEYS.TOPIC_BUFFERS],
    queryFn: () => appClient.topicBuffers.getTopicBufferSubscriptions(),
  })
}
