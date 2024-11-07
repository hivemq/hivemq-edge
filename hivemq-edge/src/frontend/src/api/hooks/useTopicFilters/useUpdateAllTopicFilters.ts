import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type TopicFilterList } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateAllTopicFiltersProps {
  requestBody?: TopicFilterList
}

export const useUpdateAllTopicFilter = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  return useMutation<UpdateAllTopicFiltersProps, ApiError, UpdateAllTopicFiltersProps>({
    mutationFn: ({ requestBody }: UpdateAllTopicFiltersProps) => {
      return appClient.topicFilters.updateTopicFilters(requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS] })
    },
  })
}
