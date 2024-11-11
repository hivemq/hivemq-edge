import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type TopicFilter } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateTopicFilterProps {
  requestBody: TopicFilter
}

export const useCreateTopicFilter = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createTopicFilter = ({ requestBody }: CreateTopicFilterProps) => {
    return appClient.topicFilters.addTopicFilters(requestBody)
  }

  return useMutation<CreateTopicFilterProps, ApiError, CreateTopicFilterProps>({
    mutationFn: createTopicFilter,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS] })
    },
  })
}
