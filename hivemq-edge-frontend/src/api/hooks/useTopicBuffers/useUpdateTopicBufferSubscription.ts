import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError, TopicBufferSubscription } from '@/api/__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateTopicBufferSubscriptionProps {
  topicFilter: string
  requestBody: TopicBufferSubscription
}

export const useUpdateTopicBufferSubscription = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  return useMutation<UpdateTopicBufferSubscriptionProps, ApiError, UpdateTopicBufferSubscriptionProps>({
    mutationFn: ({ topicFilter, requestBody }) =>
      appClient.topicBuffers.updateTopicBufferSubscription(topicFilter, requestBody),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.TOPIC_BUFFERS] })
    },
  })
}
