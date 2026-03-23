import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError, TopicBufferSubscription } from '@/api/__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateTopicBufferSubscriptionProps {
  requestBody: TopicBufferSubscription
}

export const useCreateTopicBufferSubscription = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  return useMutation<CreateTopicBufferSubscriptionProps, ApiError, CreateTopicBufferSubscriptionProps>({
    mutationFn: ({ requestBody }) => appClient.topicBuffers.addTopicBufferSubscription(requestBody),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.TOPIC_BUFFERS] })
    },
  })
}
