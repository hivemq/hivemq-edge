import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError } from '@/api/__generated__'
import { MOCK_MQTT_TOPIC_SAMPLES } from '@/__test-utils__/adapters/mqtt-subscription.mocks.ts'

interface PostTopicSamplesProps {
  adapter: string
  topic: string
}

export const useGetTopicSamples = () => {
  return useQuery<string[], ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_TOPICS],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 800))
      return []
    },
  })
}

export const usePostTopicSamples = () => {
  const queryClient = useQueryClient()

  return useMutation<undefined, ApiError, PostTopicSamplesProps>({
    mutationFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 800))
      return undefined
    },
    onMutate: () => {
      queryClient.setQueryData<string[]>([QUERY_KEYS.DISCOVERY_TOPICS], (old) => {
        return Array.from(new Set([...MOCK_MQTT_TOPIC_SAMPLES, ...(old || [])]))
      })
    },
  })
}
