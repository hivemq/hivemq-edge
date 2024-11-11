import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type TopicFilter } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateTopicFilterProps {
  name: string
  requestBody?: TopicFilter
}

export const useUpdateTopicFilter = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateTopicFilters = ({ name, requestBody }: UpdateTopicFilterProps) => {
    return appClient.topicFilters.updateTopicFilter(encodeURIComponent(name), requestBody)
  }

  return useMutation<UpdateTopicFilterProps, ApiError, UpdateTopicFilterProps>({
    mutationFn: updateTopicFilters,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS] })
    },
  })
}
