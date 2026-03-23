import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '@/api/__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface DeleteTopicBufferSubscriptionProps {
  topicFilter: string
}

export const useDeleteTopicBufferSubscription = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  return useMutation<DeleteTopicBufferSubscriptionProps, ApiError, DeleteTopicBufferSubscriptionProps>({
    mutationFn: ({ topicFilter }) => appClient.topicBuffers.deleteTopicBufferSubscription(topicFilter),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.TOPIC_BUFFERS] })
    },
  })
}
